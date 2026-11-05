package com.gensolar.engineering;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private EditText capacity, watts, length, width, gap, columns, factor;
    private TextView output;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("engineering_draft", MODE_PRIVATE);
        buildScreen();
    }

    private void buildScreen() {
        int pad = dp(18);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(pad, pad, pad, pad);
        page.setBackgroundColor(0xFFF3F6F8);

        TextView brand = text("GENSOLAR PRO", 13, 0xFF087F5B, true);
        page.addView(brand);
        TextView title = text("Engineering workspace", 26, 0xFF12202C, true);
        page.addView(title);
        TextView intro = text("Offline-first starter · Rooftop area estimator", 14, 0xFF586979, false);
        page.addView(intro);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(16), 0, dp(20));

        capacity = field(content, "Target system capacity (kW)", "10");
        watts = field(content, "Panel wattage (330–700 W)", "550");
        length = field(content, "Panel length (mm)", "2278");
        width = field(content, "Panel width (mm)", "1134");
        gap = field(content, "Provisional module gap (mm; enter approved value)", "0");
        columns = field(content, "Panels per row", "10");
        factor = field(content, "Planning area factor (1.0 or higher; provisional)", "1.0");

        Button calculate = new Button(this);
        calculate.setText("CALCULATE ROOFTOP AREA");
        calculate.setOnClickListener(v -> calculate());
        content.addView(calculate, matchWrap());

        output = text("Enter project inputs and calculate. Values are saved on this device.", 15, 0xFF12202C, false);
        output.setPadding(dp(14), dp(14), dp(14), dp(14));
        content.addView(output, matchWrap());

        TextView note = text("Engineering notice: this starter performs preliminary geometric estimates only. It does not calculate code-compliant wind loads, structural member sizes, anchors, foundations or construction approvals.", 12, 0xFF8A4B08, false);
        note.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.addView(note, matchWrap());

        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(page);
        restore();
    }

    private EditText field(LinearLayout parent, String label, String defaultValue) {
        TextView caption = text(label, 13, 0xFF334455, true);
        caption.setPadding(0, dp(12), 0, dp(4));
        parent.addView(caption, matchWrap());
        EditText edit = new EditText(this);
        edit.setSingleLine(true);
        edit.setInputType(8194);
        edit.setText(prefs.getString(label, defaultValue));
        edit.setTextSize(16);
        parent.addView(edit, matchWrap());
        if (label.startsWith("Target")) capacity = edit;
        else if (label.startsWith("Panel wattage")) watts = edit;
        else if (label.startsWith("Panel length")) length = edit;
        else if (label.startsWith("Panel width")) width = edit;
        else if (label.startsWith("Provisional")) gap = edit;
        else if (label.startsWith("Panels per")) columns = edit;
        else factor = edit;
        return edit;
    }

    private void calculate() {
        try {
            double kw = number(capacity), w = number(watts), l = number(length), b = number(width);
            double g = number(gap), f = number(factor);
            int c = (int) number(columns);
            RoofAreaCalculator.Result r = RoofAreaCalculator.calculate(kw, w, l, b, g, c, f);
            String report = String.format(Locale.US,
                    "RESULTS\n\nPanels required: %d\nInstalled capacity: %.2f kWp\nRows: %d\nLast row panels: %d\n\nPanel surface area: %.2f m² (%.2f sq.ft)\nInter-panel / row gap area: %.2f m² (%.2f sq.ft)\nFabrication footprint: %.2f m² (%.2f sq.ft)\n\nEstimated gross rooftop area: %.2f m²\nEstimated gross rooftop area: %.2f sq.ft\n\nAssumed gap: %.0f mm · Gross factor: %.2f×\n\nThis is a preliminary estimate, not an approved structural or fabrication design.",
                    r.panelCount, r.installedKwp, r.rows, r.lastRowPanels,
                    r.panelAreaSqm, r.panelAreaSqm * RoofAreaCalculator.SQFT_PER_SQM,
                    r.gapAreaSqm, r.gapAreaSqm * RoofAreaCalculator.SQFT_PER_SQM,
                    r.fabricationAreaSqm, r.fabricationAreaSqm * RoofAreaCalculator.SQFT_PER_SQM,
                    r.grossRoofAreaSqm, r.grossRoofAreaSqft, g, f);
            output.setText(report);
            prefs.edit()
                    .putString("Target system capacity (kW)", capacity.getText().toString())
                    .putString("Panel wattage (330–700 W)", watts.getText().toString())
                    .putString("Panel length (mm)", length.getText().toString())
                    .putString("Panel width (mm)", width.getText().toString())
                    .putString("Provisional module gap (mm; enter approved value)", gap.getText().toString())
                    .putString("Panels per row", columns.getText().toString())
                    .putString("Planning area factor (1.0 or higher; provisional)", factor.getText().toString())
                    .putString("last_result", report).apply();
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(capacity.getWindowToken(), 0);
        } catch (Exception e) {
            Toast.makeText(this, "Check all input values. " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void restore() {
        String previous = prefs.getString("last_result", null);
        if (previous != null) output.setText(previous);
    }
    private double number(EditText e) { return Double.parseDouble(e.getText().toString().trim()); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
}
