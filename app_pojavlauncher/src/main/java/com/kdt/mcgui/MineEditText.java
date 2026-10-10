package com.kdt.mcgui;

import android.content.*;
import android.util.*;
import android.graphics.*;
import android.widget.EditText;

public class MineEditText extends androidx.appcompat.widget.AppCompatEditText {
	public MineEditText(Context ctx) {
		super(ctx);
		init();
	}

	public MineEditText(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init();
	}

	public void init() {
		setBackgroundResource(net.kdt.pojavlaunch.R.drawable.rkb_input_background);
		int pad = Math.round(10 * getResources().getDisplayMetrics().density);
		setPadding(pad, pad / 2, pad, pad / 2);
		setTextColor(Color.WHITE);
		setHintTextColor(Color.parseColor("#7A8FA8"));
	}
}
