package net.kdt.pojavlaunch.prefs.screens;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import net.kdt.pojavlaunch.R;

import java.util.ArrayList;

/**
 * Voice settings. Real functionality only:
 *  - on/off switch (off = the launcher never opens the microphone),
 *  - microphone permission via the official Android permission API (+ app settings if permanently denied),
 *  - input language (used by the speech-recognition test),
 *  - microphone level test (AudioRecord) and speech-recognition test (Android RecognizerIntent).
 * There is NO in-game voice chat in this launcher; the screen says so.
 */
public class LauncherPreferenceVoiceFragment extends LauncherPreferenceFragment {
    private static final String KEY_ENABLED = "voice_input_enabled";
    private static final String KEY_LANG = "voice_input_language";
    private static final String KEY_ASKED = "voice_mic_asked"; // lets us tell "never asked" from "permanently denied"

    private final Handler mMain = new Handler(Looper.getMainLooper());
    private SwitchPreference mEnable;
    private Preference mPermission;
    private volatile boolean mTesting;
    private Thread mTestThread;
    private AlertDialog mTestDialog;
    private boolean mEnableAfterGrant;

    private final ActivityResultLauncher<String> mPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (mEnableAfterGrant) {
                    mEnableAfterGrant = false;
                    if (mEnable != null) mEnable.setChecked(granted); // stays off if the user said no
                }
                refreshPermission();
                if (!granted && getContext() != null) {
                    Toast.makeText(getContext(), "Microphone permission was not granted", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> mRecognitionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (getContext() == null) return;
                ArrayList<String> texts = result.getData() == null ? null
                        : result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (texts != null && !texts.isEmpty()) {
                    new AlertDialog.Builder(getContext()).setTitle("Recognized")
                            .setMessage(texts.get(0)).setPositiveButton(android.R.string.ok, null).show();
                } else {
                    Toast.makeText(getContext(), "Nothing was recognized", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public void onCreatePreferences(Bundle b, String str) {
        addPreferencesFromResource(R.xml.pref_voice);
        mEnable = requirePreference(KEY_ENABLED, SwitchPreference.class);
        mPermission = requirePreference("voice_mic_permission");

        mEnable.setOnPreferenceChangeListener((p, newValue) -> {
            boolean on = (Boolean) newValue;
            if (!on) { stopTest(); return true; }
            if (hasPermission()) return true;
            mEnableAfterGrant = true;   // switch is set once the permission result arrives
            requestOrOpenSettings();
            return false;               // do not turn on yet
        });

        mPermission.setOnPreferenceClickListener(p -> { requestOrOpenSettings(); return true; });

        requirePreference("voice_mic_test").setOnPreferenceClickListener(p -> { startMicTest(); return true; });
        requirePreference("voice_recognition_test").setOnPreferenceClickListener(p -> { startRecognitionTest(); return true; });
        refreshPermission();
    }

    // ---------------------------------------------------------------- permission

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private SharedPreferences askedPrefs() {
        return requireContext().getSharedPreferences("rkb_voice", Context.MODE_PRIVATE);
    }

    private boolean permanentlyDenied() {
        return !hasPermission() && askedPrefs().getBoolean(KEY_ASKED, false)
                && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO);
    }

    private void refreshPermission() {
        if (mPermission == null || getContext() == null) return;
        if (hasPermission()) {
            mPermission.setSummary("Granted");
        } else if (permanentlyDenied()) {
            mPermission.setSummary("Denied - tap to open the app settings");
        } else {
            mPermission.setSummary("Not granted - tap to allow");
        }
    }

    private void requestOrOpenSettings() {
        if (hasPermission()) { refreshPermission(); return; }
        if (permanentlyDenied()) {
            mEnableAfterGrant = false;
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", requireContext().getPackageName(), null));
            startActivity(i);
            return;
        }
        askedPrefs().edit().putBoolean(KEY_ASKED, true).apply();
        mPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPermission(); // the user may have changed it in the system settings
        if (mEnable != null && mEnable.isChecked() && !hasPermission()) mEnable.setChecked(false);
    }

    // ---------------------------------------------------------------- microphone test

    private boolean voiceAllowed() {
        if (mEnable == null || !mEnable.isChecked()) {
            Toast.makeText(requireContext(), "Turn on Voice input first", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!hasPermission()) { requestOrOpenSettings(); return false; }
        return true;
    }

    private void startMicTest() {
        if (!voiceAllowed() || mTesting) return;
        final Context c = requireContext();
        final ProgressBar bar = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        final TextView label = new TextView(c);
        label.setText("Speak now...");
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * c.getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);
        box.addView(label);
        box.addView(bar);
        mTestDialog = new AlertDialog.Builder(c).setTitle("Microphone test").setView(box)
                .setNegativeButton("Stop", null)
                .setOnDismissListener(d -> stopTest()).show();

        mTesting = true;
        mTestThread = new Thread(() -> runMicTest(bar, label), "RKB-MicTest");
        mTestThread.start();
    }

    private void runMicTest(ProgressBar bar, TextView label) {
        final int rate = 16000;
        int min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord rec = null;
        String error = null;
        int peakOverall = 0;
        try {
            if (min <= 0) throw new IllegalStateException("Microphone not supported on this device");
            rec = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, min * 2);
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("Microphone is unavailable or in use by another app");
            }
            rec.startRecording();
            short[] buf = new short[min];
            long end = System.currentTimeMillis() + 5000;
            while (mTesting && System.currentTimeMillis() < end) {
                int n = rec.read(buf, 0, buf.length);
                if (n <= 0) continue;
                int peak = 0;
                for (int i = 0; i < n; i++) peak = Math.max(peak, Math.abs(buf[i]));
                peakOverall = Math.max(peakOverall, peak);
                final int level = Math.min(100, peak * 100 / 12000);
                mMain.post(() -> bar.setProgress(level));
            }
        } catch (SecurityException e) {
            error = "Microphone permission was revoked";
        } catch (Exception e) {
            error = e.getMessage();
        } finally {
            if (rec != null) {
                try { rec.stop(); } catch (Exception ignored) { }
                rec.release(); // always release: never keep the microphone open
            }
            mTesting = false;
        }
        final String err = error;
        final int peak = peakOverall;
        mMain.post(() -> {
            if (mTestDialog == null || !mTestDialog.isShowing()) return;
            label.setText(err != null ? "Error: " + err
                    : (peak < 300 ? "No sound detected. Check the microphone or speak louder."
                    : "Microphone works."));
        });
    }

    private void stopTest() {
        mTesting = false; // the loop releases the AudioRecord in its finally block
        mTestThread = null;
    }

    // ---------------------------------------------------------------- recognition test

    private void startRecognitionTest() {
        if (!voiceAllowed()) return;
        String lang = ((ListPreference) requirePreference(KEY_LANG)).getValue();
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        if (lang != null && !"system".equals(lang)) i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");
        try {
            mRecognitionLauncher.launch(i);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No speech recognition service is installed on this device", Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- lifecycle: never keep the mic open

    @Override
    public void onPause() {
        stopTest();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        stopTest();
        if (mTestDialog != null) { mTestDialog.dismiss(); mTestDialog = null; }
        mMain.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }
}
