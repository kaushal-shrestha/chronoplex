package com.zoneanchor;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String STATE_SELECTED_TAB = "selected_tab";
    private static final int TAB_CLOCKS = 0;
    private static final int TAB_ALARMS = 1;
    private static final int TAB_SETTINGS = 2;
    private static final int NO_EDITING_ALARM = -1;

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm:ss a", Locale.getDefault());
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault());
    private static final DateTimeFormatter ALARM_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
    private static final DateTimeFormatter NEXT_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a z", Locale.getDefault());

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<ClockRow> clockRows = new ArrayList<>();
    private final ArrayList<AlarmRow> alarmRows = new ArrayList<>();
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refreshTimes();
            handler.postDelayed(this, 1000L);
        }
    };

    private ArrayList<ZoneChoice> zoneChoices;
    private LinearLayout screenContent;
    private LinearLayout clockList;
    private LinearLayout clockEditorContainer;
    private LinearLayout alarmList;
    private LinearLayout alarmEditorContainer;
    private LinearLayout alarmStatusPanel;
    private Button clocksTabButton;
    private Button alarmsTabButton;
    private Button settingsTabButton;
    private Button floatingAddButton;
    private Button exactAlarmButton;
    private Spinner clockZoneSpinner;
    private Spinner alarmZoneSpinner;
    private EditText clockLabelInput;
    private EditText alarmLabelInput;
    private TimePicker alarmTimePicker;
    private TextView alarmStatusView;
    private TextView permissionStatusView;
    private Switch alarmZoneSourceSwitch;
    private Spinner alarmSoundSpinner;
    private Switch alarmVibrateSwitch;
    private ArrayList<CheckBox> dayBoxes = new ArrayList<>();
    private AppColorTheme activeTheme;
    private String appearanceMode;
    private int selectedTab = TAB_CLOCKS;
    private boolean addingClock;
    private boolean addingAlarm;
    private String editingClockZoneId;
    private int editingAlarmId = NO_EDITING_ALARM;
    private boolean alarmUseClockZones = true;
    private String alarmStatusMessage = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appearanceMode = SettingsStore.loadAppearance(this);
        setTheme(shouldUseDarkMode()
                ? android.R.style.Theme_Material_NoActionBar
                : android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            selectedTab = savedInstanceState.getInt(STATE_SELECTED_TAB, TAB_CLOCKS);
        }
        activeTheme = SettingsStore.loadTheme(this).resolve(shouldUseDarkMode());
        zoneChoices = buildZoneChoices();
        NotificationHelper.ensureChannel(this);
        buildUi();
        NotificationHelper.requestNotificationPermissionIfNeeded(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_SELECTED_TAB, selectedTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPermissionState();
        refreshTimes();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(activeTheme.backgroundColor);
        applySystemBarColors();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(activeTheme.backgroundColor);
        root.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        applySystemBarPadding(content, dp(20), dp(20), dp(20), dp(96));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView title = text("ZoneAnchor Alarm", 30, Typeface.BOLD, activeTheme.primaryTextColor);
        content.addView(title);

        TextView subtitle = text("Pin clocks and alarms to the time zones you choose.", 16, Typeface.NORMAL, activeTheme.secondaryTextColor);
        subtitle.setPadding(0, dp(6), 0, dp(18));
        content.addView(subtitle);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, 0, 0, dp(14));
        clocksTabButton = tabButton("Clocks", TAB_CLOCKS);
        alarmsTabButton = tabButton("Alarms", TAB_ALARMS);
        settingsTabButton = tabButton("Settings", TAB_SETTINGS);
        tabs.addView(clocksTabButton);
        tabs.addView(alarmsTabButton);
        tabs.addView(settingsTabButton);
        content.addView(tabs);

        screenContent = new LinearLayout(this);
        screenContent.setOrientation(LinearLayout.VERTICAL);
        content.addView(screenContent, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        floatingAddButton = squareButton("+", activeTheme.accentColor, activeTheme.accentTextColor);
        floatingAddButton.setTextSize(26);
        floatingAddButton.setContentDescription("Add");
        floatingAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAddFlow();
            }
        });
        FrameLayout.LayoutParams addButtonParams = new FrameLayout.LayoutParams(
                dp(56),
                dp(56),
                Gravity.BOTTOM | Gravity.END
        );
        root.addView(floatingAddButton, addButtonParams);
        applyFloatingButtonInsets(floatingAddButton, dp(20), dp(20));

        setContentView(root);
        showTab(selectedTab);
    }

    private void showTab(int tab) {
        selectedTab = tab;
        if (selectedTab == TAB_CLOCKS) {
            addingAlarm = false;
            editingAlarmId = NO_EDITING_ALARM;
        } else if (selectedTab == TAB_ALARMS) {
            addingClock = false;
            editingClockZoneId = null;
        } else {
            addingClock = false;
            addingAlarm = false;
            editingClockZoneId = null;
            editingAlarmId = NO_EDITING_ALARM;
        }
        styleTab(clocksTabButton, selectedTab == TAB_CLOCKS);
        styleTab(alarmsTabButton, selectedTab == TAB_ALARMS);
        styleTab(settingsTabButton, selectedTab == TAB_SETTINGS);
        if (selectedTab == TAB_CLOCKS) {
            buildClocksTab();
        } else if (selectedTab == TAB_ALARMS) {
            buildAlarmsTab();
        } else {
            buildSettingsTab();
        }
        refreshTimes();
        refreshPermissionState();
        updateFloatingAddButton();
    }

    private void showAddFlow() {
        if (selectedTab == TAB_CLOCKS) {
            addingClock = true;
            editingClockZoneId = null;
            buildClocksTab();
        } else if (selectedTab == TAB_ALARMS) {
            addingAlarm = true;
            editingAlarmId = NO_EDITING_ALARM;
            alarmStatusMessage = "";
            buildAlarmsTab();
        }
        updateFloatingAddButton();
    }

    private void buildClocksTab() {
        screenContent.removeAllViews();
        clockRows.clear();
        alarmRows.clear();
        exactAlarmButton = null;
        alarmStatusView = null;
        permissionStatusView = null;
        alarmStatusPanel = null;
        alarmList = null;
        alarmEditorContainer = null;
        alarmZoneSourceSwitch = null;
        alarmLabelInput = null;
        alarmSoundSpinner = null;
        alarmVibrateSwitch = null;
        editingAlarmId = NO_EDITING_ALARM;
        dayBoxes.clear();

        clockEditorContainer = new LinearLayout(this);
        clockEditorContainer.setOrientation(LinearLayout.VERTICAL);
        screenContent.addView(clockEditorContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        renderClockEditor();

        clockList = new LinearLayout(this);
        clockList.setOrientation(LinearLayout.VERTICAL);
        screenContent.addView(clockList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        renderClockRows();
        updateFloatingAddButton();
    }

    private void buildAlarmsTab() {
        screenContent.removeAllViews();
        clockRows.clear();
        alarmRows.clear();
        clockList = null;
        clockZoneSpinner = null;
        clockEditorContainer = null;
        clockLabelInput = null;
        editingClockZoneId = null;

        alarmStatusPanel = panel();

        permissionStatusView = text("", 14, Typeface.NORMAL, activeTheme.secondaryTextColor);
        alarmStatusPanel.addView(permissionStatusView);

        exactAlarmButton = secondaryButton("Enable exact alarms");
        exactAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openExactAlarmSettings();
            }
        });
        alarmStatusPanel.addView(exactAlarmButton);

        alarmStatusView = text(alarmStatusMessage, 14, Typeface.NORMAL, activeTheme.secondaryTextColor);
        alarmStatusView.setPadding(0, dp(8), 0, 0);
        alarmStatusPanel.addView(alarmStatusView);
        screenContent.addView(alarmStatusPanel);

        alarmEditorContainer = new LinearLayout(this);
        alarmEditorContainer.setOrientation(LinearLayout.VERTICAL);
        screenContent.addView(alarmEditorContainer);
        renderAlarmEditor();

        alarmList = new LinearLayout(this);
        alarmList.setOrientation(LinearLayout.VERTICAL);
        screenContent.addView(alarmList, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        renderAlarmRows();
        refreshPermissionState();
        updateFloatingAddButton();
    }

    private void buildSettingsTab() {
        screenContent.removeAllViews();
        clockRows.clear();
        alarmRows.clear();
        exactAlarmButton = null;
        alarmStatusView = null;
        permissionStatusView = null;
        alarmStatusPanel = null;
        clockList = null;
        clockEditorContainer = null;
        alarmList = null;
        alarmEditorContainer = null;
        clockZoneSpinner = null;
        alarmZoneSpinner = null;
        clockLabelInput = null;
        alarmLabelInput = null;
        alarmZoneSourceSwitch = null;
        alarmSoundSpinner = null;
        alarmVibrateSwitch = null;
        editingClockZoneId = null;
        editingAlarmId = NO_EDITING_ALARM;
        dayBoxes.clear();

        TextView appearanceSection = sectionLabel("Appearance");
        appearanceSection.setPadding(0, 0, 0, dp(8));
        screenContent.addView(appearanceSection);
        screenContent.addView(appearanceOption(
                SettingsStore.APPEARANCE_SYSTEM,
                "Follow system",
                "Match Android's current light or dark setting."
        ));
        screenContent.addView(appearanceOption(
                SettingsStore.APPEARANCE_LIGHT,
                "Light mode",
                "Keep ZoneAnchor Alarm bright regardless of system setting."
        ));
        screenContent.addView(appearanceOption(
                SettingsStore.APPEARANCE_DARK,
                "Dark mode",
                "Use a dark app surface for low-light use."
        ));

        TextView section = sectionLabel("Theme");
        section.setPadding(0, 0, 0, dp(8));
        screenContent.addView(section);

        for (AppColorTheme theme : AppColorTheme.all()) {
            screenContent.addView(themeOption(theme));
        }
        updateFloatingAddButton();
    }

    private LinearLayout appearanceOption(final String mode, String title, String descriptionText) {
        LinearLayout option = panel();
        option.addView(text(title, 20, Typeface.BOLD, activeTheme.primaryTextColor));

        TextView description = text(descriptionText, 14, Typeface.NORMAL, activeTheme.secondaryTextColor);
        description.setPadding(0, dp(4), 0, dp(10));
        option.addView(description);

        boolean selected = appearanceMode.equals(mode);
        Button chooseButton = selected ? secondaryButton("Selected") : primaryButton("Use this mode");
        chooseButton.setEnabled(!selected);
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsStore.saveAppearance(MainActivity.this, mode);
                appearanceMode = mode;
                selectedTab = TAB_SETTINGS;
                recreate();
            }
        });
        option.addView(chooseButton);
        return option;
    }

    private LinearLayout themeOption(final AppColorTheme theme) {
        AppColorTheme previewTheme = theme.resolve(shouldUseDarkMode());
        LinearLayout option = panel();
        option.addView(text(theme.name, 20, Typeface.BOLD, activeTheme.primaryTextColor));

        TextView description = text(theme.description, 14, Typeface.NORMAL, activeTheme.secondaryTextColor);
        description.setPadding(0, dp(4), 0, dp(10));
        option.addView(description);

        LinearLayout swatches = new LinearLayout(this);
        swatches.setOrientation(LinearLayout.HORIZONTAL);
        swatches.setPadding(0, 0, 0, dp(8));
        addSwatch(swatches, previewTheme.backgroundColor);
        addSwatch(swatches, previewTheme.surfaceColor);
        addSwatch(swatches, previewTheme.accentColor);
        option.addView(swatches);

        boolean selected = activeTheme.id.equals(theme.id);
        Button chooseButton = selected ? secondaryButton("Selected") : primaryButton("Use this theme");
        chooseButton.setEnabled(!selected);
        chooseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsStore.saveTheme(MainActivity.this, theme);
                activeTheme = theme.resolve(shouldUseDarkMode());
                buildUi();
            }
        });
        option.addView(chooseButton);
        return option;
    }

    private void addSwatch(LinearLayout row, int color) {
        View swatch = new View(this);
        swatch.setBackground(roundedBackground(color, activeTheme.borderColor));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(44), dp(28));
        params.setMargins(0, 0, dp(8), 0);
        row.addView(swatch, params);
    }

    private void renderClockEditor() {
        if (clockEditorContainer == null) {
            return;
        }
        clockEditorContainer.removeAllViews();
        if (!addingClock) {
            return;
        }

        ZoneClockStore.ClockEntry editingClock = editingClockZoneId == null
                ? null
                : findClockEntry(editingClockZoneId);
        if (editingClockZoneId != null && editingClock == null) {
            editingClockZoneId = null;
        }
        LinearLayout editor = panel();
        editor.addView(sectionLabel(editingClock == null ? "New clock" : "Edit clock"));

        clockZoneSpinner = new Spinner(this);
        clockZoneSpinner.setAdapter(zoneAdapter());
        clockZoneSpinner.setPadding(0, dp(8), 0, dp(8));
        editor.addView(clockZoneSpinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        if (editingClock != null) {
            selectZoneChoice(clockZoneSpinner, editingClock.zoneId);
        }

        clockLabelInput = new EditText(this);
        clockLabelInput.setSingleLine(true);
        clockLabelInput.setHint("Custom name");
        clockLabelInput.setTextSize(16);
        clockLabelInput.setTextColor(activeTheme.primaryTextColor);
        clockLabelInput.setHintTextColor(activeTheme.secondaryTextColor);
        clockLabelInput.setPadding(0, dp(8), 0, dp(8));
        if (editingClock != null) {
            clockLabelInput.setText(editingClock.label);
        }
        editor.addView(clockLabelInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        Button saveButton = primaryButton(editingClock == null ? "Save clock" : "Update clock");
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addSelectedClock();
            }
        });
        editor.addView(saveButton);

        Button cancelButton = secondaryButton("Cancel");
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addingClock = false;
                editingClockZoneId = null;
                buildClocksTab();
            }
        });
        editor.addView(cancelButton);
        clockEditorContainer.addView(editor);
    }

    private void renderClockRows() {
        if (clockList == null) {
            return;
        }
        clockList.removeAllViews();
        clockRows.clear();
        addClockRow("Device time", null, false);
        for (ZoneClockStore.ClockEntry entry : ZoneClockStore.load(this)) {
            addClockRow(entry.displayName(), entry.zoneId, true);
        }
        refreshTimes();
    }

    private void addClockRow(String label, final String zoneId, boolean removable) {
        LinearLayout card = panel();
        card.addView(sectionLabel(label));
        TextView timeView = text("", 34, Typeface.BOLD, zoneId == null ? activeTheme.primaryTextColor : activeTheme.accentColor);
        TextView detailView = text("", 14, Typeface.NORMAL, activeTheme.secondaryTextColor);
        card.addView(timeView);
        card.addView(detailView);
        if (removable) {
            Button editButton = secondaryButton("Edit");
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addingClock = true;
                    editingClockZoneId = zoneId;
                    buildClocksTab();
                }
            });
            card.addView(editButton);

            Button removeButton = secondaryButton("Remove");
            removeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (zoneId != null && zoneId.equals(editingClockZoneId)) {
                        addingClock = false;
                        editingClockZoneId = null;
                    }
                    ZoneClockStore.remove(MainActivity.this, zoneId);
                    renderClockRows();
                }
            });
            card.addView(removeButton);
        }
        clockList.addView(card);
        clockRows.add(new ClockRow(zoneId, timeView, detailView));
    }

    private void renderAlarmEditor() {
        if (alarmEditorContainer == null) {
            return;
        }
        alarmEditorContainer.removeAllViews();
        if (!addingAlarm) {
            return;
        }

        ZonedAlarm editingAlarm = editingAlarmId == NO_EDITING_ALARM
                ? null
                : AlarmStore.load(this, editingAlarmId);
        if (editingAlarmId != NO_EDITING_ALARM && editingAlarm == null) {
            editingAlarmId = NO_EDITING_ALARM;
        }
        LinearLayout editor = panel();
        editor.addView(sectionLabel(editingAlarm == null ? "New alarm" : "Edit alarm"));

        alarmLabelInput = new EditText(this);
        alarmLabelInput.setSingleLine(true);
        alarmLabelInput.setHint("Alarm name");
        alarmLabelInput.setTextSize(16);
        alarmLabelInput.setTextColor(activeTheme.primaryTextColor);
        alarmLabelInput.setHintTextColor(activeTheme.secondaryTextColor);
        alarmLabelInput.setPadding(0, dp(8), 0, dp(8));
        if (editingAlarm != null) {
            alarmLabelInput.setText(editingAlarm.label);
        }
        editor.addView(alarmLabelInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        alarmZoneSourceSwitch = new Switch(this);
        alarmZoneSourceSwitch.setText("Use clocks tab zones");
        alarmZoneSourceSwitch.setTextSize(16);
        alarmZoneSourceSwitch.setTextColor(activeTheme.primaryTextColor);
        alarmZoneSourceSwitch.setPadding(0, dp(4), 0, dp(8));
        boolean hasClockZones = !ZoneClockStore.load(this).isEmpty();
        if (!hasClockZones) {
            alarmUseClockZones = false;
        } else if (editingAlarm != null) {
            alarmUseClockZones = hasClockZone(editingAlarm.zoneId);
        }
        alarmZoneSourceSwitch.setEnabled(hasClockZones);
        alarmZoneSourceSwitch.setChecked(alarmUseClockZones);
        alarmZoneSourceSwitch.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                alarmUseClockZones = isChecked;
                updateAlarmZoneChoices();
            }
        });
        editor.addView(alarmZoneSourceSwitch);

        alarmZoneSpinner = new Spinner(this);
        alarmZoneSpinner.setPadding(0, dp(8), 0, dp(8));
        editor.addView(alarmZoneSpinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        updateAlarmZoneChoices();
        if (editingAlarm != null) {
            selectAlarmZoneChoice(editingAlarm.zoneId);
        }

        alarmTimePicker = new TimePicker(this);
        alarmTimePicker.setIs24HourView(false);
        alarmTimePicker.setHour(editingAlarm == null ? ZonedAlarm.DEFAULT_HOUR : editingAlarm.hour);
        alarmTimePicker.setMinute(editingAlarm == null ? ZonedAlarm.DEFAULT_MINUTE : editingAlarm.minute);
        alarmTimePicker.setPadding(0, dp(4), 0, dp(8));
        editor.addView(alarmTimePicker, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        editor.addView(sectionLabel("Days"));
        addDayPicker(editor, editingAlarm == null ? ZonedAlarm.ALL_DAYS : editingAlarm.daysOfWeekMask);

        editor.addView(sectionLabel("Alert"));
        alarmSoundSpinner = new Spinner(this);
        alarmSoundSpinner.setAdapter(soundAdapter());
        alarmSoundSpinner.setPadding(0, dp(8), 0, dp(8));
        editor.addView(alarmSoundSpinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        if (editingAlarm != null) {
            selectSoundChoice(editingAlarm.soundMode);
        }

        alarmVibrateSwitch = new Switch(this);
        alarmVibrateSwitch.setText("Vibrate");
        alarmVibrateSwitch.setTextSize(16);
        alarmVibrateSwitch.setTextColor(activeTheme.primaryTextColor);
        alarmVibrateSwitch.setChecked(editingAlarm == null || editingAlarm.vibrate);
        alarmVibrateSwitch.setPadding(0, dp(4), 0, dp(8));
        editor.addView(alarmVibrateSwitch);

        Button saveButton = primaryButton(editingAlarm == null ? "Save alarm" : "Update alarm");
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAlarmFromEditor();
            }
        });
        editor.addView(saveButton);

        Button cancelButton = secondaryButton("Cancel");
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addingAlarm = false;
                editingAlarmId = NO_EDITING_ALARM;
                buildAlarmsTab();
            }
        });
        editor.addView(cancelButton);
        alarmEditorContainer.addView(editor);
    }

    private void addDayPicker(LinearLayout editor, int selectedDaysMask) {
        dayBoxes.clear();
        LinearLayout firstRow = dayRow();
        LinearLayout secondRow = dayRow();
        String[] labels = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        int days = selectedDaysMask == 0 ? ZonedAlarm.ALL_DAYS : selectedDaysMask;
        for (int i = 0; i < labels.length; i++) {
            CheckBox box = new CheckBox(this);
            box.setText(labels[i]);
            box.setTextSize(14);
            box.setTextColor(activeTheme.primaryTextColor);
            box.setChecked((days & (1 << i)) != 0);
            box.setPadding(0, 0, 0, 0);
            dayBoxes.add(box);
            if (i < 4) {
                firstRow.addView(box, dayBoxParams());
            } else {
                secondRow.addView(box, dayBoxParams());
            }
        }
        editor.addView(firstRow);
        editor.addView(secondRow);
    }

    private LinearLayout dayRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, dp(6));
        return row;
    }

    private LinearLayout.LayoutParams dayBoxParams() {
        return new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
    }

    private void renderAlarmRows() {
        if (alarmList == null) {
            return;
        }
        alarmList.removeAllViews();
        alarmRows.clear();

        ArrayList<ZonedAlarm> alarms = AlarmStore.loadAll(this);
        if (alarms.isEmpty()) {
            LinearLayout empty = panel();
            empty.addView(text("No alarms yet.", 16, Typeface.NORMAL, activeTheme.secondaryTextColor));
            alarmList.addView(empty);
            return;
        }

        for (final ZonedAlarm alarm : alarms) {
            LinearLayout card = panel();
            card.addView(sectionLabel(alarm.displayName()));

            TextView timeView = text(alarmTimeText(alarm), 34, Typeface.BOLD, activeTheme.primaryTextColor);
            card.addView(timeView);

            TextView settingsView = text(alarmSettingsText(alarm), 15, Typeface.NORMAL, activeTheme.secondaryTextColor);
            settingsView.setPadding(0, dp(4), 0, 0);
            card.addView(settingsView);

            TextView nextView = text("", 15, Typeface.NORMAL, activeTheme.secondaryTextColor);
            nextView.setPadding(0, dp(4), 0, 0);
            card.addView(nextView);

            Button editButton = secondaryButton("Edit");
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addingAlarm = true;
                    editingAlarmId = alarm.id;
                    alarmStatusMessage = "";
                    buildAlarmsTab();
                }
            });
            card.addView(editButton);

            Button cancelButton = secondaryButton("Cancel alarm");
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (alarm.id == editingAlarmId) {
                        addingAlarm = false;
                        editingAlarmId = NO_EDITING_ALARM;
                    }
                    AlarmScheduler.cancel(MainActivity.this, alarm.id);
                    alarmStatusMessage = "Alarm canceled.";
                    buildAlarmsTab();
                }
            });
            card.addView(cancelButton);
            alarmList.addView(card);
            alarmRows.add(new AlarmRow(alarm.id, nextView));
        }
        refreshTimes();
    }

    private void addSelectedClock() {
        ZoneChoice zoneChoice = (ZoneChoice) clockZoneSpinner.getSelectedItem();
        if (zoneChoice == null) {
            return;
        }
        String label = clockLabelInput == null ? "" : clockLabelInput.getText().toString();
        if (editingClockZoneId == null) {
            ZoneClockStore.add(this, zoneChoice.zoneId, label);
        } else {
            ZoneClockStore.update(this, editingClockZoneId, zoneChoice.zoneId, label);
        }
        addingClock = false;
        editingClockZoneId = null;
        buildClocksTab();
    }

    private void addAlarmFromEditor() {
        AlarmZoneChoice zoneChoice = (AlarmZoneChoice) alarmZoneSpinner.getSelectedItem();
        if (zoneChoice == null) {
            return;
        }
        int daysOfWeekMask = selectedDaysMask();
        if (daysOfWeekMask == 0) {
            alarmStatusMessage = "Pick at least one day.";
            if (alarmStatusView != null) {
                alarmStatusView.setText(alarmStatusMessage);
            }
            updateAlarmStatusPanelVisibility();
            return;
        }
        SoundChoice soundChoice = (SoundChoice) alarmSoundSpinner.getSelectedItem();
        String soundMode = soundChoice == null ? ZonedAlarm.SOUND_DEFAULT : soundChoice.soundMode;
        String label = alarmLabelInput == null ? "" : alarmLabelInput.getText().toString();
        int alarmId = editingAlarmId == NO_EDITING_ALARM ? AlarmStore.nextId(this) : editingAlarmId;

        ZonedAlarm alarm = new ZonedAlarm(
                alarmId,
                label,
                zoneChoice.zoneId,
                alarmTimePicker.getHour(),
                alarmTimePicker.getMinute(),
                daysOfWeekMask,
                soundMode,
                alarmVibrateSwitch == null || alarmVibrateSwitch.isChecked(),
                true,
                0L
        );
        AlarmScheduler.ScheduleResult result = AlarmScheduler.scheduleNext(this, alarm);
        alarmStatusMessage = result.message;
        if (result.scheduled) {
            addingAlarm = false;
            editingAlarmId = NO_EDITING_ALARM;
        }
        buildAlarmsTab();
    }

    private int selectedDaysMask() {
        int mask = 0;
        for (int i = 0; i < dayBoxes.size(); i++) {
            if (dayBoxes.get(i).isChecked()) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    private ZoneClockStore.ClockEntry findClockEntry(String zoneId) {
        for (ZoneClockStore.ClockEntry entry : ZoneClockStore.load(this)) {
            if (entry.zoneId.equals(zoneId)) {
                return entry;
            }
        }
        return null;
    }

    private boolean hasClockZone(String zoneId) {
        return findClockEntry(zoneId) != null;
    }

    private void updateAlarmZoneChoices() {
        if (alarmZoneSpinner == null) {
            return;
        }

        ArrayList<AlarmZoneChoice> choices = alarmUseClockZones
                ? buildClockAlarmZoneChoices()
                : buildAllAlarmZoneChoices();
        if (choices.isEmpty()) {
            alarmUseClockZones = false;
            choices = buildAllAlarmZoneChoices();
            if (alarmZoneSourceSwitch != null) {
                alarmZoneSourceSwitch.setChecked(false);
            }
        }

        ArrayAdapter<AlarmZoneChoice> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                choices
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        alarmZoneSpinner.setAdapter(adapter);
    }

    private void selectZoneChoice(Spinner spinner, String zoneId) {
        if (spinner == null) {
            return;
        }
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item instanceof ZoneChoice && ((ZoneChoice) item).zoneId.equals(zoneId)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void selectAlarmZoneChoice(String zoneId) {
        if (alarmZoneSpinner == null) {
            return;
        }
        for (int i = 0; i < alarmZoneSpinner.getCount(); i++) {
            Object item = alarmZoneSpinner.getItemAtPosition(i);
            if (item instanceof AlarmZoneChoice && ((AlarmZoneChoice) item).zoneId.equals(zoneId)) {
                alarmZoneSpinner.setSelection(i);
                return;
            }
        }
    }

    private void selectSoundChoice(String soundMode) {
        if (alarmSoundSpinner == null) {
            return;
        }
        for (int i = 0; i < alarmSoundSpinner.getCount(); i++) {
            Object item = alarmSoundSpinner.getItemAtPosition(i);
            if (item instanceof SoundChoice && ((SoundChoice) item).soundMode.equals(soundMode)) {
                alarmSoundSpinner.setSelection(i);
                return;
            }
        }
    }

    private void refreshTimes() {
        Instant now = Instant.now();
        for (ClockRow row : clockRows) {
            ZoneId zone = row.zoneId == null ? ZoneId.systemDefault() : ZoneId.of(row.zoneId);
            ZonedDateTime zonedTime = now.atZone(zone);
            row.timeView.setText(TIME_FORMAT.format(zonedTime));
            row.detailView.setText(zone.getId() + " - " + DATE_FORMAT.format(zonedTime));
        }

        for (AlarmRow row : alarmRows) {
            ZonedAlarm alarm = AlarmStore.load(this, row.alarmId);
            if (alarm == null || !alarm.enabled || alarm.nextTriggerAtMillis <= 0L) {
                row.nextView.setText("Not scheduled.");
            } else {
                row.nextView.setText(nextAlarmText(alarm));
            }
        }
    }

    private void refreshPermissionState() {
        if (exactAlarmButton == null || permissionStatusView == null) {
            return;
        }

        boolean canSchedule = AlarmScheduler.canScheduleExactAlarms(this);
        exactAlarmButton.setVisibility(canSchedule ? View.GONE : View.VISIBLE);
        if (!canSchedule) {
            permissionStatusView.setText("Exact alarm permission is required for precise locked-time alerts.");
            permissionStatusView.setVisibility(View.VISIBLE);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionStatusView.setText("Notification permission is needed so the alarm can alert you.");
            permissionStatusView.setVisibility(View.VISIBLE);
        } else {
            permissionStatusView.setText("");
            permissionStatusView.setVisibility(View.GONE);
        }
        if (alarmStatusView != null) {
            boolean hasStatus = alarmStatusMessage != null && !alarmStatusMessage.isEmpty();
            alarmStatusView.setText(hasStatus ? alarmStatusMessage : "");
            alarmStatusView.setVisibility(hasStatus ? View.VISIBLE : View.GONE);
        }
        updateAlarmStatusPanelVisibility();
    }

    private void updateAlarmStatusPanelVisibility() {
        if (alarmStatusPanel == null) {
            return;
        }
        boolean showPanel = exactAlarmButton != null && exactAlarmButton.getVisibility() == View.VISIBLE;
        showPanel = showPanel || (permissionStatusView != null && permissionStatusView.getVisibility() == View.VISIBLE);
        showPanel = showPanel || (alarmStatusView != null && alarmStatusView.getVisibility() == View.VISIBLE);
        alarmStatusPanel.setVisibility(showPanel ? View.VISIBLE : View.GONE);
    }

    private String alarmTimeText(ZonedAlarm alarm) {
        return ALARM_TIME_FORMAT.format(LocalTime.of(alarm.hour, alarm.minute));
    }

    private String alarmSettingsText(ZonedAlarm alarm) {
        String alert = ZonedAlarm.SOUND_SILENT.equals(alarm.soundMode) ? "Silent" : "Default sound";
        if (alarm.vibrate) {
            alert += " + vibration";
        }
        return alarm.zoneId + " - " + daysText(alarm.daysOfWeekMask) + " - " + alert;
    }

    private String daysText(int daysOfWeekMask) {
        int days = daysOfWeekMask == 0 ? ZonedAlarm.ALL_DAYS : daysOfWeekMask;
        if ((days & ZonedAlarm.ALL_DAYS) == ZonedAlarm.ALL_DAYS) {
            return "Every day";
        }
        if (days == 0b0011111) {
            return "Weekdays";
        }
        if (days == 0b1100000) {
            return "Weekends";
        }

        String[] labels = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < labels.length; i++) {
            if ((days & (1 << i)) != 0) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(labels[i]);
            }
        }
        return builder.toString();
    }

    private String nextAlarmText(ZonedAlarm alarm) {
        ZonedDateTime trigger = Instant.ofEpochMilli(alarm.nextTriggerAtMillis).atZone(ZoneId.of(alarm.zoneId));
        return "Next: " + NEXT_FORMAT.format(trigger);
    }

    private void openExactAlarmSettings() {
        try {
            startActivity(AlarmScheduler.exactAlarmSettingsIntent(this));
        } catch (ActivityNotFoundException ignored) {
            Intent fallback = new Intent(android.provider.Settings.ACTION_SETTINGS);
            startActivity(fallback);
        }
    }

    private ArrayAdapter<ZoneChoice> zoneAdapter() {
        ArrayAdapter<ZoneChoice> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                zoneChoices
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private ArrayAdapter<SoundChoice> soundAdapter() {
        ArrayList<SoundChoice> choices = new ArrayList<>();
        choices.add(new SoundChoice(ZonedAlarm.SOUND_DEFAULT, "Default alarm sound"));
        choices.add(new SoundChoice(ZonedAlarm.SOUND_SILENT, "Silent"));
        ArrayAdapter<SoundChoice> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                choices
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private ArrayList<AlarmZoneChoice> buildClockAlarmZoneChoices() {
        ArrayList<AlarmZoneChoice> choices = new ArrayList<>();
        for (ZoneClockStore.ClockEntry entry : ZoneClockStore.load(this)) {
            choices.add(new AlarmZoneChoice(entry.zoneId, entry.displayName()));
        }
        return choices;
    }

    private ArrayList<AlarmZoneChoice> buildAllAlarmZoneChoices() {
        ArrayList<AlarmZoneChoice> choices = new ArrayList<>();
        for (ZoneChoice zoneChoice : zoneChoices) {
            choices.add(new AlarmZoneChoice(zoneChoice.zoneId, ""));
        }
        return choices;
    }

    private ArrayList<ZoneChoice> buildZoneChoices() {
        Set<String> ids = new LinkedHashSet<>();
        Collections.addAll(ids,
                "America/New_York",
                "America/Chicago",
                "America/Denver",
                "America/Los_Angeles",
                "UTC",
                "Europe/London",
                "Europe/Paris",
                "Asia/Kathmandu",
                "Asia/Tokyo",
                "Australia/Sydney"
        );
        ids.add(ZoneId.systemDefault().getId());
        ids.addAll(ZoneId.getAvailableZoneIds());

        ArrayList<ZoneChoice> choices = new ArrayList<>();
        for (String id : ids) {
            choices.add(new ZoneChoice(id));
        }

        Collections.sort(choices, new Comparator<ZoneChoice>() {
            @Override
            public int compare(ZoneChoice left, ZoneChoice right) {
                return left.zoneId.compareTo(right.zoneId);
            }
        });

        moveToTop(choices, "UTC");
        moveToTop(choices, ZonedAlarm.DEFAULT_ZONE_ID);
        moveToTop(choices, ZoneId.systemDefault().getId());
        return choices;
    }

    private void moveToTop(ArrayList<ZoneChoice> choices, String zoneId) {
        for (int i = 0; i < choices.size(); i++) {
            if (choices.get(i).zoneId.equals(zoneId)) {
                choices.add(0, choices.remove(i));
                return;
            }
        }
    }

    private LinearLayout panel() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable background = new GradientDrawable();
        background.setColor(activeTheme.surfaceColor);
        background.setCornerRadius(dp(8));
        background.setStroke(dp(1), activeTheme.borderColor);
        layout.setBackground(background);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(12));
        layout.setLayoutParams(params);
        return layout;
    }

    private TextView sectionLabel(String value) {
        TextView view = text(value, 13, Typeface.BOLD, activeTheme.secondaryTextColor);
        view.setAllCaps(true);
        view.setPadding(0, 0, 0, dp(8));
        return view;
    }

    private TextView text(String value, int sp, int typefaceStyle, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, typefaceStyle);
        view.setIncludeFontPadding(true);
        view.setLetterSpacing(0f);
        return view;
    }

    private Button tabButton(String label, final int tab) {
        Button button = button(label, Color.TRANSPARENT, activeTheme.primaryTextColor);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
        );
        params.setMargins(dp(3), 0, dp(3), 0);
        button.setLayoutParams(params);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTab(tab);
            }
        });
        return button;
    }

    private void styleTab(Button button, boolean selected) {
        int fill = selected ? activeTheme.tabSelectedColor : activeTheme.tabUnselectedColor;
        int stroke = selected ? activeTheme.tabSelectedColor : activeTheme.borderColor;
        int textColor = selected ? activeTheme.tabSelectedTextColor : activeTheme.tabUnselectedTextColor;
        button.setTextColor(textColor);
        button.setBackground(roundedBackground(fill, stroke));
    }

    private void updateFloatingAddButton() {
        if (floatingAddButton == null) {
            return;
        }
        if (selectedTab == TAB_SETTINGS) {
            floatingAddButton.setVisibility(View.GONE);
            return;
        }
        boolean adding = selectedTab == TAB_CLOCKS ? addingClock : addingAlarm;
        floatingAddButton.setVisibility(adding ? View.GONE : View.VISIBLE);
        floatingAddButton.setContentDescription(selectedTab == TAB_CLOCKS ? "Add clock" : "Add alarm");
    }

    private boolean shouldUseDarkMode() {
        if (SettingsStore.APPEARANCE_DARK.equals(appearanceMode)) {
            return true;
        }
        if (SettingsStore.APPEARANCE_LIGHT.equals(appearanceMode)) {
            return false;
        }
        int uiMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return uiMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private Button primaryButton(String label) {
        Button button = button(label, activeTheme.accentColor, activeTheme.accentTextColor);
        LinearLayout.LayoutParams params = buttonParams();
        params.setMargins(0, dp(8), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = button(label, Color.TRANSPARENT, activeTheme.primaryTextColor);
        LinearLayout.LayoutParams params = buttonParams();
        params.setMargins(0, dp(8), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private Button squareButton(String label, int fillColor, int textColor) {
        Button button = button(label, fillColor, textColor);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(48), dp(48));
        button.setLayoutParams(params);
        return button;
    }

    private Button button(String label, int fillColor, int textColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(16);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundedBackground(
                fillColor,
                fillColor == Color.TRANSPARENT ? activeTheme.borderColor : fillColor
        ));
        return button;
    }

    private GradientDrawable roundedBackground(int fillColor, int strokeColor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fillColor);
        background.setCornerRadius(dp(8));
        background.setStroke(dp(1), strokeColor);
        return background;
    }

    private LinearLayout.LayoutParams buttonParams() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(52)
        );
    }

    @SuppressWarnings("deprecation")
    private void applySystemBarColors() {
        getWindow().setStatusBarColor(activeTheme.backgroundColor);
        getWindow().setNavigationBarColor(activeTheme.backgroundColor);

        int flags = 0;
        if (activeTheme.lightSystemBars && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (activeTheme.lightSystemBars && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @SuppressWarnings("deprecation")
    private void applySystemBarPadding(
            final View view,
            final int left,
            final int top,
            final int right,
            final int bottom
    ) {
        view.setPadding(left, top, right, bottom);
        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View insetView, WindowInsets insets) {
                int topInset;
                int bottomInset;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.graphics.Insets systemInsets = insets.getInsets(
                            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                    );
                    topInset = systemInsets.top;
                    bottomInset = systemInsets.bottom;
                } else {
                    topInset = insets.getSystemWindowInsetTop();
                    bottomInset = insets.getSystemWindowInsetBottom();
                }
                insetView.setPadding(left, top + topInset, right, bottom + bottomInset);
                return insets;
            }
        });
        view.requestApplyInsets();
    }

    @SuppressWarnings("deprecation")
    private void applyFloatingButtonInsets(
            final View view,
            final int right,
            final int bottom
    ) {
        view.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View insetView, WindowInsets insets) {
                int rightInset;
                int bottomInset;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.graphics.Insets systemInsets = insets.getInsets(
                            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                    );
                    rightInset = systemInsets.right;
                    bottomInset = systemInsets.bottom;
                } else {
                    rightInset = insets.getSystemWindowInsetRight();
                    bottomInset = insets.getSystemWindowInsetBottom();
                }

                ViewGroup.LayoutParams currentParams = insetView.getLayoutParams();
                if (currentParams instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) currentParams;
                    params.setMargins(0, 0, right + rightInset, bottom + bottomInset);
                    insetView.setLayoutParams(params);
                }
                return insets;
            }
        });
        view.requestApplyInsets();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class ClockRow {
        final String zoneId;
        final TextView timeView;
        final TextView detailView;

        ClockRow(String zoneId, TextView timeView, TextView detailView) {
            this.zoneId = zoneId;
            this.timeView = timeView;
            this.detailView = detailView;
        }
    }

    private static final class AlarmRow {
        final int alarmId;
        final TextView nextView;

        AlarmRow(int alarmId, TextView nextView) {
            this.alarmId = alarmId;
            this.nextView = nextView;
        }
    }

    private static final class AlarmZoneChoice {
        final String zoneId;
        final String label;

        AlarmZoneChoice(String zoneId, String label) {
            this.zoneId = zoneId;
            this.label = label == null ? "" : label.trim();
        }

        @Override
        public String toString() {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zoneId));
            String shortZone = DateTimeFormatter.ofPattern("z", Locale.getDefault()).format(now);
            if (label.isEmpty() || label.equals(zoneId)) {
                return zoneId + "  " + shortZone;
            }
            return label + " - " + zoneId + "  " + shortZone;
        }
    }

    private static final class SoundChoice {
        final String soundMode;
        final String label;

        SoundChoice(String soundMode, String label) {
            this.soundMode = soundMode;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final class ZoneChoice {
        final String zoneId;

        ZoneChoice(String zoneId) {
            this.zoneId = zoneId;
        }

        @Override
        public String toString() {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zoneId));
            String shortZone = DateTimeFormatter.ofPattern("z", Locale.getDefault()).format(now);
            return zoneId + "  " + shortZone;
        }
    }
}
