package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.rkb.discord.DiscordRPC;

/**
 * Account picker — CS Launcher style:
 * Microsoft / Local / Link Discord (Rich Presence)
 */
public class SelectAuthFragment extends Fragment {
    public static final String TAG = "AUTH_SELECT_FRAGMENT";

    public SelectAuthFragment(){
        super(R.layout.fragment_select_auth_method);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button mMicrosoftButton = view.findViewById(R.id.button_microsoft_authentication);
        Button mLocalButton = view.findViewById(R.id.button_local_authentication);
        Button mDiscordButton = view.findViewById(R.id.button_discord_link);

        // Microsoft stays the filled neon primary action; the others are outlined navy cards.
        for (Button b : new Button[]{mMicrosoftButton, mLocalButton, mDiscordButton}) {
            b.setAllCaps(false);
            b.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
            b.setPadding(Math.round(18 * getResources().getDisplayMetrics().density), 0,
                    Math.round(18 * getResources().getDisplayMetrics().density), 0);
        }
        mLocalButton.setBackgroundResource(R.drawable.rkb_item_card);
        mDiscordButton.setBackgroundResource(R.drawable.rkb_item_card);

        mMicrosoftButton.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), MicrosoftLoginFragment.class, MicrosoftLoginFragment.TAG, null));

        mLocalButton.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), LocalLoginFragment.class, LocalLoginFragment.TAG, null));

        // CS-style: Link Discord account → OAuth + Rich Presence
        mDiscordButton.setOnClickListener(v -> {
            if (DiscordRPC.isLinked(requireContext())) {
                String user = DiscordRPC.getLinkedUser(requireContext());
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.rkb_discord_rpc_title)
                        .setMessage(getString(R.string.rkb_discord_connected, user))
                        .setPositiveButton(R.string.rkb_discord_unlink, (d, w) -> {
                            DiscordRPC.unlink(requireContext());
                            refreshDiscordButton(mDiscordButton);
                            Toast.makeText(requireContext(), R.string.rkb_discord_unlink, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            } else {
                DiscordRPC.startLink(requireContext());
            }
        });

        refreshDiscordButton(mDiscordButton);
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) {
            Button btn = v.findViewById(R.id.button_discord_link);
            if (btn != null) refreshDiscordButton(btn);
        }
    }

    private void refreshDiscordButton(Button btn) {
        if (DiscordRPC.isLinked(requireContext())) {
            String user = DiscordRPC.getLinkedUser(requireContext());
            btn.setText(getString(R.string.rkb_discord_connected, user));
        } else {
            btn.setText(R.string.rkb_discord_link);
        }
    }
}
