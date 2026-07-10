package com.charles.rotationcontrol;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private RuleStore ruleStore;
    private AppListAdapter adapter;
    private final List<AppInfoItem> allApps = new ArrayList<>();
    private final ExecutorService loader = Executors.newSingleThreadExecutor();

    private View setupCard;
    private TextView statusAccessibility;
    private TextView statusWriteSettings;
    private TextView ruleSummary;
    private MaterialSwitch masterSwitch;
    private MaterialSwitch rulesOnlySwitch;
    private TextInputEditText searchInput;
    private View loadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ruleStore = new RuleStore(this);

        setupCard = findViewById(R.id.setup_card);
        statusAccessibility = findViewById(R.id.status_accessibility);
        statusWriteSettings = findViewById(R.id.status_write_settings);
        ruleSummary = findViewById(R.id.rule_summary);
        masterSwitch = findViewById(R.id.master_switch);
        rulesOnlySwitch = findViewById(R.id.rules_only_switch);
        searchInput = findViewById(R.id.search_input);
        loadingView = findViewById(R.id.loading);
        RecyclerView list = findViewById(R.id.app_list);

        adapter = new AppListAdapter(this::showOrientationPicker);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        masterSwitch.setChecked(ruleStore.isMasterEnabled());
        masterSwitch.setOnCheckedChangeListener(this::onMasterToggled);

        findViewById(R.id.btn_open_accessibility).setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        findViewById(R.id.btn_open_write_settings).setOnClickListener(v -> openWriteSettings());

        rulesOnlySwitch.setOnCheckedChangeListener((button, checked) -> applyFilter());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSetupStatus();
        if (RotationAccessibilityService.isRunning()) {
            RotationAccessibilityService svc = RotationAccessibilityService.getInstance();
            if (svc != null) {
                svc.applyCurrentForeground();
            }
        }
    }

    @Override
    protected void onDestroy() {
        loader.shutdownNow();
        super.onDestroy();
    }

    private void onMasterToggled(CompoundButton button, boolean checked) {
        ruleStore.setMasterEnabled(checked);
        RotationAccessibilityService svc = RotationAccessibilityService.getInstance();
        if (!checked) {
            // Prefer the live service applier so the accessibility overlay is removed.
            if (svc != null) {
                svc.forceRestore();
            } else {
                new OrientationApplier(this).forceRestore();
            }
            Toast.makeText(this, R.string.master_off_toast, Toast.LENGTH_SHORT).show();
        } else if (svc != null) {
            svc.applyCurrentForeground();
        }
        refreshSetupStatus();
    }

    private void openWriteSettings() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + getPackageName())
        );
        startActivity(intent);
    }

    private void refreshSetupStatus() {
        boolean a11y = RotationAccessibilityService.isRunning();
        boolean write = Settings.System.canWrite(this);

        statusAccessibility.setText(a11y
                ? R.string.status_accessibility_on
                : R.string.status_accessibility_off);
        statusAccessibility.setTextColor(getColor(a11y ? R.color.status_ok : R.color.status_warn));

        statusWriteSettings.setText(write
                ? R.string.status_write_on
                : R.string.status_write_off);
        statusWriteSettings.setTextColor(getColor(write ? R.color.status_ok : R.color.status_warn));

        // Accessibility is required (overlay force). Write-settings is recommended
        // but optional — Auto still works via the accessibility overlay alone.
        setupCard.setVisibility(a11y ? View.GONE : View.VISIBLE);

        int rules = ruleStore.ruleCount();
        String master = ruleStore.isMasterEnabled()
                ? getString(R.string.master_on)
                : getString(R.string.master_off);
        ruleSummary.setText(getString(R.string.rule_summary_fmt, rules, master));
    }

    private void loadApps() {
        loadingView.setVisibility(View.VISIBLE);
        loader.execute(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppInfoItem> result = new ArrayList<>();
            for (ApplicationInfo info : installed) {
                if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        && (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                    Intent launch = pm.getLaunchIntentForPackage(info.packageName);
                    if (launch == null) {
                        continue;
                    }
                } else {
                    Intent launch = pm.getLaunchIntentForPackage(info.packageName);
                    if (launch == null && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        continue;
                    }
                    if (launch == null) {
                        continue;
                    }
                }
                if (getPackageName().equals(info.packageName)) {
                    continue;
                }
                CharSequence labelCs = pm.getApplicationLabel(info);
                String label = labelCs != null ? labelCs.toString() : info.packageName;
                android.graphics.drawable.Drawable icon;
                try {
                    icon = pm.getApplicationIcon(info);
                } catch (Exception e) {
                    icon = getDrawable(R.drawable.ic_launcher);
                }
                OrientationMode mode = ruleStore.getMode(info.packageName);
                result.add(new AppInfoItem(info.packageName, label, icon, mode));
            }
            Collections.sort(result, (a, b) ->
                    a.label.compareToIgnoreCase(b.label));

            runOnUiThread(() -> {
                allApps.clear();
                allApps.addAll(result);
                loadingView.setVisibility(View.GONE);
                applyFilter();
                refreshSetupStatus();
            });
        });
    }

    private void applyFilter() {
        String query = "";
        if (searchInput.getText() != null) {
            query = searchInput.getText().toString().trim().toLowerCase(Locale.US);
        }
        boolean rulesOnly = rulesOnlySwitch.isChecked();
        List<AppInfoItem> filtered = new ArrayList<>();
        for (AppInfoItem item : allApps) {
            if (rulesOnly && item.mode == OrientationMode.SYSTEM) {
                continue;
            }
            if (!query.isEmpty()) {
                String hay = (item.label + " " + item.packageName).toLowerCase(Locale.US);
                if (!hay.contains(query)) {
                    continue;
                }
            }
            filtered.add(item);
        }
        adapter.submit(filtered);
    }

    private void showOrientationPicker(AppInfoItem item) {
        OrientationMode[] modes = OrientationMode.values();
        String[] labels = new String[modes.length];
        int selected = 0;
        for (int i = 0; i < modes.length; i++) {
            labels[i] = modes[i].label;
            if (modes[i] == item.mode) {
                selected = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(item.label)
                .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                    OrientationMode chosen = modes[which];
                    ruleStore.setMode(item.packageName, chosen);
                    item.mode = chosen;
                    adapter.notifyDataSetChanged();
                    refreshSetupStatus();
                    if (RotationAccessibilityService.isRunning()) {
                        RotationAccessibilityService svc = RotationAccessibilityService.getInstance();
                        if (svc != null) {
                            svc.applyCurrentForeground();
                        }
                    }
                    Toast.makeText(
                            this,
                            getString(R.string.rule_set_toast, item.label, chosen.label),
                            Toast.LENGTH_SHORT
                    ).show();
                    dialog.dismiss();
                    applyFilter();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
