/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.test.latency;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ui.RectangleInsets;

/**
 * Chart writer for latency results.
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @since 5.0.5
 */
public class ChartWriter {

  private XYDataset dataset;
  private String xAxisLabel;
  private String yAxisLabel;
  private String chartTitle;

  public ChartWriter(Map<Long, LatencyRegistry> latencyMap, String seriestTitle) {
    this(latencyMap, seriestTitle, "Latency Control", "Remote Tag Time (s)", "Lantecy (ms)");
  }

  public ChartWriter(Map<Long, LatencyRegistry> latencyMap, String seriesTitle, String chartTitle,
      String xAxisLabel, String yAxisLabel) {

    // Convert latencyMap to XYDataset
    XYSeries series = new XYSeries(seriesTitle);
    for (long time : latencyMap.keySet()) {
      series.add(time, Math.abs(latencyMap.get(time).getLatency()));
    }
    dataset = new XYSeriesCollection();
    ((XYSeriesCollection) dataset).addSeries(series);

    this.xAxisLabel = xAxisLabel;
    this.yAxisLabel = yAxisLabel;
    this.chartTitle = chartTitle;
  }

  public void drawChart(String filename, int width, int height) throws IOException {
    // Create plot
    NumberAxis xAxis = new NumberAxis(xAxisLabel);
    NumberAxis yAxis = new NumberAxis(yAxisLabel);
    XYSplineRenderer renderer = new XYSplineRenderer();
    XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
    plot.setBackgroundPaint(Color.lightGray);
    plot.setDomainGridlinePaint(Color.white);
    plot.setRangeGridlinePaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(4, 4, 4, 4));

    // Create chart
    JFreeChart chart = new JFreeChart(chartTitle, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
    ChartUtils.applyCurrentTheme(chart);
    ChartPanel chartPanel = new ChartPanel(chart, false);

    // Draw png
    BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
    Graphics graphics = bi.getGraphics();
    chartPanel.setBounds(0, 0, width, height);
    chartPanel.paint(graphics);
    ImageIO.write(bi, "png", new File(filename));
  }

}
