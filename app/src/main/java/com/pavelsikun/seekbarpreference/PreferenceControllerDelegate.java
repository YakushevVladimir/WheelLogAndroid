package com.pavelsikun.seekbarpreference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.cooper.wheellog.R;

/**
 * Created by Pavel Sikun on 28.05.16.
 */
class PreferenceControllerDelegate implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private final String TAG = getClass().getSimpleName();

    private static final int DEFAULT_CURRENT_VALUE = 50;
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;
    private static final int DEFAULT_INTERVAL = 1;
    private static final boolean DEFAULT_DIALOG_ENABLED = true;
    private static final boolean DEFAULT_IS_ENABLED = true;

    private static final int DEFAULT_DIALOG_STYLE = R.style.MSB_Dialog_Default;

    private int maxValue;
    private int minValue;
    private int interval;
    private int currentValue;
    private String measurementUnit;
    private boolean dialogEnabled;

    private int dialogStyle;

    private TextView valueView;
    private SeekBar seekBarView;
    private TextView measurementView;
    private View bottomLineView;

    //view stuff
    private TextView titleView, summaryView;
    private String title;
    private String summary;
    private boolean isEnabled;

    //controller stuff
    private boolean isView;
    private Context context;
    private ViewStateListener viewStateListener;
    private PersistValueListener persistValueListener;
    private ChangeValueListener changeValueListener;

    interface ViewStateListener {

        boolean isEnabled();

        void setEnabled(boolean enabled);
    }

    PreferenceControllerDelegate(Context context, Boolean isView) {
        this.context = context;
        this.isView = isView;
    }

    void setPersistValueListener(PersistValueListener persistValueListener) {
        this.persistValueListener = persistValueListener;
    }

    void setViewStateListener(ViewStateListener viewStateListener) {
        this.viewStateListener = viewStateListener;
    }

    void setChangeValueListener(ChangeValueListener changeValueListener) {
        this.changeValueListener = changeValueListener;
    }

    void loadValuesFromXml(AttributeSet attrs) {
        if (attrs == null) {
            currentValue = DEFAULT_CURRENT_VALUE;
            minValue = DEFAULT_MIN_VALUE;
            maxValue = DEFAULT_MAX_VALUE;
            interval = DEFAULT_INTERVAL;
            dialogEnabled = DEFAULT_DIALOG_ENABLED;

            isEnabled = DEFAULT_IS_ENABLED;
        } else {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);
            try {
                minValue = a.getInt(R.styleable.SeekBarPreference_msbp_minValue, DEFAULT_MIN_VALUE);
                interval = a.getInt(R.styleable.SeekBarPreference_msbp_interval, DEFAULT_INTERVAL);
                int saved_maxValue = a.getInt(R.styleable.SeekBarPreference_msbp_maxValue, DEFAULT_MAX_VALUE);
                maxValue = (saved_maxValue - minValue) / interval;
                dialogEnabled = a.getBoolean(R.styleable.SeekBarPreference_msbp_dialogEnabled, DEFAULT_DIALOG_ENABLED);

                measurementUnit = a.getString(R.styleable.SeekBarPreference_msbp_measurementUnit);
                currentValue = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "defaultValue", DEFAULT_CURRENT_VALUE);

                dialogStyle = DEFAULT_DIALOG_STYLE;

                if (isView) {
                    title = a.getString(R.styleable.SeekBarPreference_msbp_view_title);
                    summary = a.getString(R.styleable.SeekBarPreference_msbp_view_summary);
                    currentValue = a.getInt(R.styleable.SeekBarPreference_msbp_view_defaultValue, DEFAULT_CURRENT_VALUE);
                    isEnabled = a.getBoolean(R.styleable.SeekBarPreference_msbp_view_enabled, DEFAULT_IS_ENABLED);
                }
            } finally {
                a.recycle();
            }
        }
    }


    void onBind(View view) {

        if (isView) {
            titleView = view.findViewById(android.R.id.title);
            summaryView = view.findViewById(android.R.id.summary);
            titleView.setText(title);
            summaryView.setText(summary);
        }

        view.setClickable(false);

        seekBarView = view.findViewById(R.id.seekbar);
        measurementView = view.findViewById(R.id.measurement_unit);
        valueView = view.findViewById(R.id.seekbar_value);

        setMaxValue(maxValue);

        seekBarView.setOnSeekBarChangeListener(this);

        measurementView.setText(measurementUnit);
        measurementView.setVisibility(measurementUnit == null || measurementUnit.isEmpty() ? View.GONE : View.VISIBLE);

        setCurrentValue(currentValue);
        valueView.setText(String.valueOf(currentValue));

        bottomLineView = view.findViewById(R.id.bottom_line);

        setDialogEnabled(dialogEnabled);
        setEnabled(isEnabled(), true);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int newValue = minValue + (progress * interval);

        if (changeValueListener != null) {
            if (!changeValueListener.onChange(newValue)) {
                return;
            }
        }
        currentValue = newValue;
        valueView.setText(String.valueOf(newValue));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        setCurrentValue(currentValue);
    }

    @Override
    public void onClick(final View v) {
        new CustomValueDialog(context, dialogStyle, minValue, maxValue, currentValue)
                .setPersistValueListener(new PersistValueListener() {
                    @Override
                    public boolean persistInt(int value) {
                        setCurrentValue(value);
                        seekBarView.setOnSeekBarChangeListener(null);
                        seekBarView.setProgress(currentValue - minValue);
                        seekBarView.setOnSeekBarChangeListener(PreferenceControllerDelegate.this);

                        valueView.setText(String.valueOf(currentValue));
                        return true;
                    }
                })
                .show();
    }


    String getTitle() {
        return title;
    }

    void setTitle(String title) {
        this.title = title;
        if (titleView != null) {
            titleView.setText(title);
        }
    }

    String getSummary() {
        return summary;
    }

    void setSummary(String summary) {
        this.summary = summary;
        if (seekBarView != null) {
            summaryView.setText(summary);
        }
    }

    boolean isEnabled() {
        if (!isView && viewStateListener != null) {
            return viewStateListener.isEnabled();
        } else {
            return isEnabled;
        }
    }

    private void setEnabled(boolean enabled, boolean viewsOnly) {
        Log.d(TAG, "setEnabled = " + enabled);
        isEnabled = enabled;

        if (viewStateListener != null && !viewsOnly) {
            viewStateListener.setEnabled(enabled);
        }

        if (seekBarView != null) { //theoretically might not always work
            Log.d(TAG, "view is disabled!");
            seekBarView.setEnabled(enabled);
            valueView.setEnabled(enabled);
            valueView.setClickable(enabled);
            measurementView.setEnabled(enabled);
            measurementView.setClickable(enabled);
            bottomLineView.setEnabled(enabled);
            if (isView) {
                titleView.setEnabled(enabled);
                summaryView.setEnabled(enabled);
            }
        }
    }

    void setEnabled(boolean enabled) {
        setEnabled(enabled, false);
    }

    int getMaxValue() {
        return maxValue;
    }

    void setMaxValue(int maxValue) {
        this.maxValue = maxValue;

        if (seekBarView != null) {
            if (minValue <= 0 && maxValue >= 0) {
                seekBarView.setMax(maxValue - minValue);
            } else {
                seekBarView.setMax(maxValue);
            }

            seekBarView.setProgress(currentValue - minValue);
        }
    }

    int getMinValue() {
        return minValue;
    }

    void setMinValue(int minValue) {
        this.minValue = minValue;
        setMaxValue(maxValue);
    }

    int getInterval() {
        return interval;
    }

    void setInterval(int interval) {
        this.interval = interval;
    }

    int getCurrentValue() {
        return currentValue;
    }

    void setCurrentValue(int value) {
        if (value < minValue) value = minValue;
        if (value > maxValue) value = maxValue;

        if (changeValueListener != null) {
            if (!changeValueListener.onChange(value)) {
                return;
            }
        }
        currentValue = value;
        if (seekBarView != null)
            seekBarView.setProgress(currentValue - minValue);

        if (persistValueListener != null) {
            persistValueListener.persistInt(value);
        }
    }

    String getMeasurementUnit() {
        return measurementUnit;
    }

    void setMeasurementUnit(String measurementUnit) {
        this.measurementUnit = measurementUnit;
        if (measurementView != null) {
            measurementView.setText(measurementUnit);
        }
    }

    boolean isDialogEnabled() {
        return dialogEnabled;
    }

    void setDialogEnabled(boolean dialogEnabled) {
        this.dialogEnabled = dialogEnabled;

        if (valueView != null && bottomLineView != null) {
            valueView.setOnClickListener(dialogEnabled ? this : null);
            measurementView.setOnClickListener(dialogEnabled ? this : null);
            valueView.setClickable(dialogEnabled);
            measurementView.setClickable(dialogEnabled);
            bottomLineView.setVisibility(dialogEnabled ? View.VISIBLE : View.INVISIBLE);
        }
    }

    void setDialogStyle(int dialogStyle) {
        this.dialogStyle = dialogStyle;
    }
}
