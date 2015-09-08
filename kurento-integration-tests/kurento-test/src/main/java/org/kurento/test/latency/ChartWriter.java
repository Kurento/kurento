/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

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

	public ChartWriter(Map<Long, LatencyRegistry> latencyMap,
			String chartTitle) {
		this(latencyMap, chartTitle, "Remote Tag Time (s)", "Lantecy (ms)");
	}

	public ChartWriter(Map<Long, LatencyRegistry> latencyMap, String chartTitle,
			String xAxisLabel, String yAxisLabel) {

		// Convert latencyMap to XYDataset
		XYSeries series = new XYSeries(chartTitle);
		for (long time : latencyMap.keySet()) {
			series.add(time, Math.abs(latencyMap.get(time).getLatency()));
		}
		dataset = new XYSeriesCollection();
		((XYSeriesCollection) dataset).addSeries(series);

		this.xAxisLabel = xAxisLabel;
		this.yAxisLabel = yAxisLabel;
	}

	public void drawChart(String filename, int width, int height)
			throws IOException {
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
		JFreeChart chart = new JFreeChart("Latency Control",
				JFreeChart.DEFAULT_TITLE_FONT, plot, true);
		ChartUtilities.applyCurrentTheme(chart);
		ChartPanel chartPanel = new ChartPanel(chart, false);

		// Draw png
		BufferedImage bi = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_BGR);
		Graphics graphics = bi.getGraphics();
		chartPanel.setBounds(0, 0, width, height);
		chartPanel.paint(graphics);
		ImageIO.write(bi, "png", new File(filename));
	}

}
