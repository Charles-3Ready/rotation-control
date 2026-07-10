package com.charles.rotationcontrol;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

/**
 * First-launch tutorial. Permissions can be granted on page 2; Enforce on page 4.
 * Skip / later / finish all mark onboarding done.
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final int PAGE_COUNT = 4;

    private RuleStore ruleStore;
    private FrameLayout pageContainer;
    private View[] dots;
    private MaterialButton btnBack;
    private MaterialButton btnPrimary;
    private MaterialButton btnSecondary;
    private int page;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ruleStore = new RuleStore(this);
        if (ruleStore.isOnboardingDone()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_onboarding);
        pageContainer = findViewById(R.id.page_container);
        dots = new View[]{
                findViewById(R.id.dot0),
                findViewById(R.id.dot1),
                findViewById(R.id.dot2),
                findViewById(R.id.dot3)
        };
        btnBack = findViewById(R.id.btn_back);
        btnPrimary = findViewById(R.id.btn_primary);
        btnSecondary = findViewById(R.id.btn_secondary);

        btnBack.setOnClickListener(v -> {
            if (page > 0) {
                showPage(page - 1);
            }
        });
        btnPrimary.setOnClickListener(v -> onPrimary());
        btnSecondary.setOnClickListener(v -> finishOnboarding());

        showPage(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (page == 1) {
            bindPermissionsPage();
        } else if (page == 3) {
            bindEnforcePage();
        }
        updateFooter();
    }

    @Override
    public void onBackPressed() {
        if (page > 0) {
            showPage(page - 1);
        } else {
            finishOnboarding();
        }
    }

    private void onPrimary() {
        if (page == 1) {
            // Prefer sending user to the first missing grant, else continue.
            if (!isAccessibilityEnabled()) {
                openAccessibility();
                return;
            }
            if (!Settings.System.canWrite(this)) {
                openWriteSettings();
                return;
            }
        }
        if (page < PAGE_COUNT - 1) {
            showPage(page + 1);
        } else {
            finishOnboarding();
        }
    }

    private void showPage(int index) {
        page = index;
        pageContainer.removeAllViews();
        LayoutInflater.from(this).inflate(pageLayout(index), pageContainer, true);

        for (int i = 0; i < dots.length; i++) {
            boolean on = i == page;
            dots[i].setBackgroundResource(on
                    ? R.drawable.bg_onboard_dot_on
                    : R.drawable.bg_onboard_dot_off);
            android.widget.LinearLayout.LayoutParams llp =
                    new android.widget.LinearLayout.LayoutParams(
                            on ? dp(18) : dp(7),
                            dp(7)
                    );
            llp.setMargins(dp(3), 0, dp(3), 0);
            dots[i].setLayoutParams(llp);
        }

        if (page == 1) {
            bindPermissionsPage();
        } else if (page == 3) {
            bindEnforcePage();
        }

        updateFooter();
    }

    private void updateFooter() {
        btnBack.setVisibility(page == 0 ? View.GONE : View.VISIBLE);
        if (page == 0) {
            btnPrimary.setText(R.string.onboard_continue);
            btnSecondary.setText(R.string.onboard_skip);
            btnSecondary.setVisibility(View.VISIBLE);
        } else if (page == 1) {
            boolean ready = isAccessibilityEnabled() && Settings.System.canWrite(this);
            btnPrimary.setText(ready ? R.string.onboard_all_set : R.string.onboard_continue_setup);
            btnSecondary.setText(R.string.onboard_later);
            btnSecondary.setVisibility(View.VISIBLE);
        } else if (page == PAGE_COUNT - 1) {
            btnPrimary.setText(R.string.onboard_done);
            btnSecondary.setVisibility(View.GONE);
        } else {
            btnPrimary.setText(R.string.onboard_continue);
            btnSecondary.setVisibility(View.GONE);
        }
    }

    private void bindPermissionsPage() {
        View root = pageContainer.getChildAt(0);
        if (root == null) {
            return;
        }
        LinearLayout cardA11y = root.findViewById(R.id.card_a11y);
        LinearLayout cardWrite = root.findViewById(R.id.card_write);
        TextView pillA11y = root.findViewById(R.id.pill_a11y);
        TextView pillWrite = root.findViewById(R.id.pill_write);
        TextView statusA11y = root.findViewById(R.id.status_a11y);
        TextView statusWrite = root.findViewById(R.id.status_write);
        MaterialButton btnA11y = root.findViewById(R.id.btn_grant_a11y);
        MaterialButton btnWrite = root.findViewById(R.id.btn_grant_write);
        if (cardA11y == null || btnA11y == null) {
            return;
        }

        boolean a11y = isAccessibilityEnabled();
        boolean write = Settings.System.canWrite(this);

        applyPermCard(cardA11y, pillA11y, statusA11y, btnA11y, a11y,
                R.string.status_accessibility_on,
                R.string.status_accessibility_off,
                R.string.open_accessibility,
                true);
        applyPermCard(cardWrite, pillWrite, statusWrite, btnWrite, write,
                R.string.status_write_on,
                R.string.status_write_off,
                R.string.open_write_settings,
                false);

        btnA11y.setOnClickListener(v -> openAccessibility());
        btnWrite.setOnClickListener(v -> openWriteSettings());
    }

    private void applyPermCard(
            LinearLayout card,
            TextView pill,
            TextView status,
            MaterialButton button,
            boolean granted,
            int statusOn,
            int statusOff,
            int actionLabel,
            boolean required
    ) {
        card.setBackgroundResource(granted
                ? R.drawable.bg_onboard_card_ok
                : R.drawable.bg_onboard_card_warn);
        if (granted) {
            pill.setText(R.string.onboard_granted);
            pill.setBackgroundResource(R.drawable.bg_onboard_pill_ok);
            pill.setTextColor(getColor(R.color.status_ok));
            status.setText(statusOn);
            status.setTextColor(getColor(R.color.status_ok));
            button.setVisibility(View.GONE);
        } else {
            pill.setText(required ? R.string.onboard_required : R.string.onboard_recommended);
            pill.setBackgroundResource(R.drawable.bg_onboard_pill);
            pill.setTextColor(getColor(R.color.status_warn));
            status.setText(statusOff);
            status.setTextColor(getColor(R.color.status_warn));
            button.setVisibility(View.VISIBLE);
            button.setText(actionLabel);
        }
    }

    private void bindEnforcePage() {
        View root = pageContainer.getChildAt(0);
        if (root == null) {
            return;
        }
        MaterialSwitch sw = root.findViewById(R.id.switch_enforce);
        TextView summary = root.findViewById(R.id.setup_summary);
        if (sw == null) {
            return;
        }
        sw.setOnCheckedChangeListener(null);
        sw.setChecked(ruleStore.isMasterEnabled());
        sw.setOnCheckedChangeListener((button, checked) -> {
            ruleStore.setMasterEnabled(checked);
            Toast.makeText(
                    this,
                    checked ? R.string.master_on : R.string.master_off_toast,
                    Toast.LENGTH_SHORT
            ).show();
            refreshEnforceSummary(summary);
        });
        refreshEnforceSummary(summary);
    }

    private void refreshEnforceSummary(TextView summary) {
        if (summary == null) {
            return;
        }
        boolean a11y = isAccessibilityEnabled();
        boolean write = Settings.System.canWrite(this);
        if (a11y && write) {
            summary.setText(R.string.onboard_setup_summary_ok);
            summary.setTextColor(getColor(R.color.status_ok));
        } else {
            StringBuilder missing = new StringBuilder();
            if (!a11y) {
                missing.append(getString(R.string.onboard_2_a11y_title));
            }
            if (!write) {
                if (missing.length() > 0) {
                    missing.append(", ");
                }
                missing.append(getString(R.string.onboard_2_write_title));
            }
            summary.setText(getString(R.string.onboard_setup_summary_missing, missing.toString()));
            summary.setTextColor(getColor(R.color.status_warn));
        }
    }

    private boolean isAccessibilityEnabled() {
        if (RotationAccessibilityService.isRunning()) {
            return true;
        }
        // Fallback: check secure setting (service may not have rebound yet).
        try {
            String enabled = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            if (enabled == null) {
                return false;
            }
            String me = getPackageName() + "/" + RotationAccessibilityService.class.getName();
            String meShort = getPackageName() + "/.RotationAccessibilityService";
            return enabled.contains(me) || enabled.contains(meShort)
                    || enabled.contains("RotationAccessibilityService");
        } catch (Exception e) {
            return false;
        }
    }

    private void openAccessibility() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, R.string.open_accessibility, Toast.LENGTH_SHORT).show();
        }
    }

    private void openWriteSettings() {
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + getPackageName())
        );
        try {
            startActivity(intent);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS));
        }
    }

    private static int pageLayout(int index) {
        switch (index) {
            case 1:
                return R.layout.onboard_page_permissions;
            case 2:
                return R.layout.onboard_page_rules;
            case 3:
                return R.layout.onboard_page_enforce;
            case 0:
            default:
                return R.layout.onboard_page_welcome;
        }
    }

    private void finishOnboarding() {
        ruleStore.setOnboardingDone(true);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
