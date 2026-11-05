package com.gensolar.engineering;

/** Preliminary geometric area calculations. Not a structural design tool. */
public final class RoofAreaCalculator {
    private RoofAreaCalculator() {}

    public static final double SQFT_PER_SQM = 10.7639104167;

    public static Result calculate(
            double targetKw,
            double panelWatts,
            double panelLengthMm,
            double panelWidthMm,
            double gapMm,
            int panelsPerRow,
            double grossAreaFactor) {

        if (!positive(targetKw) || panelWatts < 330 || panelWatts > 700
                || !positive(panelLengthMm) || !positive(panelWidthMm)
                || gapMm < 0 || panelsPerRow < 1 || grossAreaFactor < 1) {
            throw new IllegalArgumentException("Please enter valid capacity, panel, gap and layout values.");
        }

        int panelCount = (int) Math.ceil((targetKw * 1000.0) / panelWatts);
        int rows = (int) Math.ceil(panelCount / (double) panelsPerRow);
        int lastRowPanels = panelCount - (rows - 1) * panelsPerRow;

        double panelArea = panelCount * panelLengthMm * panelWidthMm / 1_000_000.0;
        double horizontalGapCount = Math.max(0, rows - 1) * Math.max(0, panelsPerRow - 1)
                + Math.max(0, lastRowPanels - 1);
        double horizontalGaps = horizontalGapCount * gapMm * panelWidthMm / 1_000_000.0;
        double rowGapArea = Math.max(0, rows - 1) * gapMm
                * panelLengthMm * panelsPerRow / 1_000_000.0;
        double fabricationArea = panelArea + horizontalGaps + rowGapArea;
        double grossRoofArea = fabricationArea * grossAreaFactor;

        return new Result(panelCount, rows, lastRowPanels,
                panelCount * panelWatts / 1000.0,
                panelArea, horizontalGaps + rowGapArea,
                fabricationArea, grossRoofArea,
                grossRoofArea * SQFT_PER_SQM);
    }

    private static boolean positive(double value) { return value > 0 && Double.isFinite(value); }

    public static final class Result {
        public final int panelCount, rows, lastRowPanels;
        public final double installedKwp, panelAreaSqm, gapAreaSqm;
        public final double fabricationAreaSqm, grossRoofAreaSqm, grossRoofAreaSqft;

        Result(int panelCount, int rows, int lastRowPanels, double installedKwp,
               double panelAreaSqm, double gapAreaSqm, double fabricationAreaSqm,
               double grossRoofAreaSqm, double grossRoofAreaSqft) {
            this.panelCount = panelCount;
            this.rows = rows;
            this.lastRowPanels = lastRowPanels;
            this.installedKwp = installedKwp;
            this.panelAreaSqm = panelAreaSqm;
            this.gapAreaSqm = gapAreaSqm;
            this.fabricationAreaSqm = fabricationAreaSqm;
            this.grossRoofAreaSqm = grossRoofAreaSqm;
            this.grossRoofAreaSqft = grossRoofAreaSqft;
        }
    }
}
