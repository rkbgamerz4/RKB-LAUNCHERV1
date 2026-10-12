package net.kdt.pojavlaunch.multirt;

import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.NewJREUtil;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.ProgressListener;
import com.kdt.mcgui.ProgressLayout;
import android.widget.Toast;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import java.io.IOException;
import java.util.List;

public class RTRecyclerViewAdapter extends RecyclerView.Adapter<RTRecyclerViewAdapter.RTViewHolder> {

    private boolean mIsDeleting = false;

    @NonNull
    @Override
    public RTViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View recyclableView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_multirt_runtime,parent,false);
        return new RTViewHolder(recyclableView);
    }

    @Override
    public void onBindViewHolder(@NonNull RTViewHolder holder, int position) {
        final List<Runtime> runtimes = MultiRTUtils.getRuntimes();
        if (position < runtimes.size()) {
            holder.bindRuntime(runtimes.get(position), position);
        } else {
            List<NewJREUtil.ExternalRuntime> downloadable = MultiRTUtils.getRuntimesToDownload();
            int index = position - runtimes.size();
            if (index < downloadable.size()) holder.bindDownloadable(downloadable.get(index));
        }
    }

    @Override
    public int getItemCount() {
        return MultiRTUtils.getRuntimes().size() + MultiRTUtils.getRuntimesToDownload().size();
    }

    public boolean isDefaultRuntime(Runtime rt) {
        return LauncherPreferences.PREF_DEFAULT_RUNTIME.equals(rt.name);
    }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    public void setDefault(Runtime rt){
        LauncherPreferences.PREF_DEFAULT_RUNTIME = rt.name;
        LauncherPreferences.DEFAULT_PREF.edit().putString("defaultRuntime",LauncherPreferences.PREF_DEFAULT_RUNTIME).apply();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    public void setIsEditing(boolean isEditing) {
        mIsDeleting = isEditing;
        notifyDataSetChanged();
    }

    public boolean getIsEditing(){
        return mIsDeleting;
    }


    public class RTViewHolder extends RecyclerView.ViewHolder {
        final TextView mJavaVersionTextView;
        final TextView mFullJavaVersionTextView;
        final ColorStateList mDefaultColors;
        final Button mSetDefaultButton;
        final ImageButton mDeleteButton;
        final Context mContext;
        Runtime mCurrentRuntime;
        NewJREUtil.ExternalRuntime mCurrentDownloadable;
        int mCurrentPosition;

        public RTViewHolder(View itemView) {
            super(itemView);
            mJavaVersionTextView = itemView.findViewById(R.id.multirt_view_java_version);
            mFullJavaVersionTextView = itemView.findViewById(R.id.multirt_view_java_version_full);
            mSetDefaultButton = itemView.findViewById(R.id.multirt_view_setdefaultbtn);
            mDeleteButton = itemView.findViewById(R.id.multirt_view_removebtn);

            mDefaultColors =  mFullJavaVersionTextView.getTextColors();
            mContext = itemView.getContext();

            setupOnClickListeners();
        }

        @SuppressLint("NotifyDataSetChanged") // same as all the other ones
        private void setupOnClickListeners(){
            mSetDefaultButton.setOnClickListener(v -> {
                if(mCurrentDownloadable != null) {
                    startRuntimeDownload(mCurrentDownloadable);
                    return;
                }
                if(mCurrentRuntime != null) {
                    setDefault(mCurrentRuntime);
                    RTRecyclerViewAdapter.this.notifyDataSetChanged();
                }
            });

            mDeleteButton.setOnClickListener(v -> {
                if (mCurrentRuntime == null) return;

                if(MultiRTUtils.getRuntimes().size() < 2) {
                    new AlertDialog.Builder(mContext)
                            .setTitle(R.string.global_error)
                            .setMessage(R.string.multirt_config_removeerror_last)
                            .setPositiveButton(android.R.string.ok,(adapter, which)->adapter.dismiss())
                            .show();
                    return;
                }

                sExecutorService.execute(() -> {
                    try {
                        MultiRTUtils.removeRuntimeNamed(mCurrentRuntime.name);
                        mDeleteButton.post(() -> {
                            if(getBindingAdapter() != null)
                                getBindingAdapter().notifyDataSetChanged();
                        });

                    } catch (IOException e) {
                        Tools.showError(itemView.getContext(), e);
                    }
                });

            });
        }

        /** A Java version that is not installed yet but can be downloaded (official OpenJDK Android build). */
        public void bindDownloadable(NewJREUtil.ExternalRuntime runtime) {
            mCurrentRuntime = null;
            mCurrentDownloadable = runtime;
            mJavaVersionTextView.setText("Java " + runtime.majorVersion);
            mFullJavaVersionTextView.setTextColor(mDefaultColors);
            mFullJavaVersionTextView.setText(runtime.isDownloading
                    ? mContext.getString(R.string.rkb_runtime_downloading)
                    : mContext.getString(R.string.rkb_runtime_not_installed));
            mDeleteButton.setVisibility(View.GONE);
            mSetDefaultButton.setVisibility(View.VISIBLE);
            mSetDefaultButton.setEnabled(!runtime.isDownloading);
            mSetDefaultButton.setText(runtime.isDownloading
                    ? R.string.rkb_runtime_downloading : R.string.rkb_runtime_download);
        }

        private void startRuntimeDownload(final NewJREUtil.ExternalRuntime runtime) {
            if (runtime.isDownloading) return; // ignore repeated taps
            runtime.isDownloading = true;
            bindDownloadable(runtime);
            final ProgressListener listener = new ProgressListener() {
                @Override public void onProgressStarted() { }
                @Override public void onProgressUpdated(int progress, int resid, Object... va) {
                    mFullJavaVersionTextView.post(() -> {
                        if (mCurrentDownloadable == runtime) {
                            mFullJavaVersionTextView.setText(mContext.getString(R.string.rkb_runtime_downloading) + " " + progress + "%");
                        }
                    });
                }
                @Override public void onProgressEnded() { }
            };
            ProgressKeeper.addListener(ProgressLayout.UNPACK_RUNTIME, listener);
            sExecutorService.execute(() -> {
                String error = null;
                try {
                    NewJREUtil.tryDownloadRuntime(mContext, runtime.majorVersion);
                } catch (Throwable t) {
                    android.util.Log.e("RTAdapter", "Runtime download failed", t);
                    error = t.getMessage() != null ? t.getMessage() : t.toString();
                }
                final String err = error;
                ProgressKeeper.removeListener(ProgressLayout.UNPACK_RUNTIME, listener);
                runtime.isDownloading = false;
                itemView.post(() -> {
                    Toast.makeText(mContext, err != null
                            ? mContext.getString(R.string.rkb_runtime_download_failed, err)
                            : mContext.getString(R.string.rkb_runtime_installed_ok, String.valueOf(runtime.majorVersion)),
                            Toast.LENGTH_LONG).show();
                    RTRecyclerViewAdapter.this.notifyDataSetChanged();
                });
            });
        }

        public void bindRuntime(Runtime runtime, int pos) {
            mCurrentDownloadable = null;
            mCurrentRuntime = runtime;
            mCurrentPosition = pos;
            if(runtime.versionString != null && Tools.DEVICE_ARCHITECTURE == Architecture.archAsInt(runtime.arch)) {
                mJavaVersionTextView.setText(runtime.name
                        .replace(".tar.xz", "")
                        .replace("-", " "));
                mFullJavaVersionTextView.setText(runtime.versionString);
                mFullJavaVersionTextView.setTextColor(mDefaultColors);

                updateButtonsVisibility();

                boolean defaultRuntime = isDefaultRuntime(runtime);
                mSetDefaultButton.setEnabled(!defaultRuntime);
                mSetDefaultButton.setText(defaultRuntime ? R.string.multirt_config_setdefault_already:R.string.multirt_config_setdefault);
                return;
            }

            // Problematic runtime moment, force propose deletion
            mDeleteButton.setVisibility(View.VISIBLE);
            if(runtime.versionString == null){
                mFullJavaVersionTextView.setText(R.string.multirt_runtime_corrupt);
            }else{
                mFullJavaVersionTextView.setText(mContext.getString(R.string.multirt_runtime_incompatiblearch, runtime.arch));
            }
            mJavaVersionTextView.setText(runtime.name);
            mFullJavaVersionTextView.setTextColor(Color.RED);
            mSetDefaultButton.setVisibility(View.GONE);
        }

        private void updateButtonsVisibility(){
            mSetDefaultButton.setVisibility(mIsDeleting ? View.GONE : View.VISIBLE);
            mDeleteButton.setVisibility(mIsDeleting ? View.VISIBLE : View.GONE);
        }
    }
}
