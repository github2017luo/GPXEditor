/*
 * Copyright (c) 2014ff Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.helper;

import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.CacheHint;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import tf.gpx.edit.general.HoveredNode;
import tf.gpx.edit.main.GPXEditorManager;


/**
 * Wrapper for gluon map to show selected waypoints
 * 
 * @author Thomas
 */
public class GPXTrackviewer {
    // don't show more than this number of points
    public final static double MAX_DATAPOINTS = 1000d;
    
    private final static GPXTrackviewer INSTANCE = new GPXTrackviewer();
    
    private final MapView myMapView;
    private final GPXWaypointChart myGPXWaypointChart;
    private final GPXWaypointLayer myGPXWaypointLayer;

    private GPXTrackviewer() {
        myMapView = new MapView();
        myMapView.setZoom(0); 
        myGPXWaypointLayer = new GPXWaypointLayer();
        myMapView.addLayer(myGPXWaypointLayer);
        myMapView.setVisible(false);
        myMapView.setCursor(Cursor.CROSSHAIR);

        myGPXWaypointChart = new GPXWaypointChart();
        myGPXWaypointChart.setLegendVisible(false);
        myGPXWaypointChart.setVisible(false);
        myGPXWaypointChart.setCursor(Cursor.CROSSHAIR);
    }
    
    public static GPXTrackviewer getInstance() {
        return INSTANCE;
    }
    
    public MapView getMapView() {
        return myMapView;
    }
    
    public XYChart getChart() {
        return myGPXWaypointChart;
    }
    
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        // show in gluon map
        myMapView.removeLayer(myGPXWaypointLayer);
        myGPXWaypointLayer.setGPXWaypoints(gpxWaypoints);
        myMapView.addLayer(myGPXWaypointLayer);
        myMapView.setCenter(myGPXWaypointLayer.getCenter());
        myMapView.setZoom(myGPXWaypointLayer.getZoom());
        myMapView.setVisible(!gpxWaypoints.isEmpty());

        // show elevation chart
        myGPXWaypointChart.setGPXWaypoints(gpxWaypoints);
        myGPXWaypointChart.setVisible(!gpxWaypoints.isEmpty());

        myGPXWaypointLayer.clearSelectedGPXWaypoints();
        myGPXWaypointChart.clearSelectedGPXWaypoints();
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        myGPXWaypointLayer.setSelectedGPXWaypoints(gpxWaypoints);
        myGPXWaypointChart.setSelectedGPXWaypoints(gpxWaypoints);
    }
}

class GPXWaypointLayer extends MapLayer {
    private final ImagePattern fileWaypointImage = 
            new ImagePattern(new Image(GPXWaypointLayer.class.getResource("/placemark_square.png").toExternalForm()));

    private final ObservableList<Triple<GPXWaypoint, Node, Line>> myPoints = FXCollections.observableArrayList();
    private final List<GPXWaypoint> selectedGPXWaypoints = new ArrayList<>();
    
    private BoundingBox myBoundingBox;

    public GPXWaypointLayer() {
        super();
        setCache(true);
        setCacheHint(CacheHint.SPEED);
    }
    
    // https://stackoverflow.com/a/42759066
    /**
     * Hack allowing to modify the default behavior of the tooltips.
     * @param openDelay The open delay, knowing that by default it is set to 1000.
     * @param visibleDuration The visible duration, knowing that by default it is set to 5000.
     * @param closeDelay The close delay, knowing that by default it is set to 200.
     * @param hideOnExit Indicates whether the tooltip should be hide on exit, 
     * knowing that by default it is set to false.
     */
    private static void updateTooltipBehavior(
            final Tooltip tooltip,
            final double openDelay,
            final double visibleDuration,
            final double closeDelay,
            final boolean hideOnExit) {
        try {
            // Get the non public field "BEHAVIOR"
            Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
            // Make the field accessible to be able to get and set its value
            fieldBehavior.setAccessible(true);
            // Get the value of the static field
            Object objBehavior = fieldBehavior.get(null);
            // Get the constructor of the private static inner class TooltipBehavior
            Constructor<?> constructor = objBehavior.getClass().getDeclaredConstructor(
                Duration.class, Duration.class, Duration.class, boolean.class
            );
            // Make the constructor accessible to be able to invoke it
            constructor.setAccessible(true);
            // Create a new instance of the private static inner class TooltipBehavior
            Object tooltipBehavior = constructor.newInstance(
                new Duration(openDelay), new Duration(visibleDuration),
                new Duration(closeDelay), hideOnExit
            );
            // Set the new instance of TooltipBehavior
            fieldBehavior.set(null, tooltipBehavior);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }    
    
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        final double ratio = GPXTrackviewer.MAX_DATAPOINTS / gpxWaypoints.size();

        // get rid of old points
        myPoints.clear();
        this.getChildren().clear();
        
        // add new points with icon and determine new bounding box
        Node prevIcon = null;
        GPXWaypoint prevWaypoint = null;
        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        double count = 0d, i = 0d;
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            i++;
            if (i * ratio >= count) {
                Shape icon;
                
                if (gpxWaypoint.isGPXFileWaypoint()) {
                    icon = new Rectangle(32, 32, Color.WHITE);
                    icon.setVisible(true);
                    icon.setStroke(Color.BLACK);
                    icon.setStrokeWidth(0);
                    // TODO: figure out way to stretch pattern to rectangle size
                    icon.setFill(fileWaypointImage);
                } else if (gpxWaypoint.isGPXRouteWaypoint()) {
                    icon = new Rectangle(6, 6, Color.DARKRED);
                    icon.setVisible(true);
                    icon.setStroke(Color.DARKBLUE);
                    icon.setStrokeWidth(0.5);
                } else {
                    icon = new Circle(3, Color.LIGHTGOLDENRODYELLOW);
                    icon.setVisible(true);
                    icon.setStroke(Color.DARKRED);
                    icon.setStrokeWidth(0.5);
                }
                
                final Tooltip tooltip = new Tooltip(gpxWaypoint.getDataAsString(GPXLineItem.GPXLineItemData.Position));
                tooltip.getStyleClass().addAll("chart-line-symbol", "chart-series-line", "track-popup");
                GPXWaypointLayer.updateTooltipBehavior(tooltip, 0, 10000, 0, true);
                
                Tooltip.install(icon, tooltip);
                
                this.getChildren().add(icon);

                Line line = null;
                // check for segment changes - we don't want lines between different segments or different routes and not for GPXFile waypoints
                // http://stackoverflow.com/questions/30879382/javafx-8-drawing-a-line-between-translated-nodes
                if (prevWaypoint != null && prevWaypoint.getParent().equals(gpxWaypoint.getParent()) && !gpxWaypoint.isGPXFileWaypoint()) {
                    line = new Line();
                    line.setVisible(true);
                    line.setStrokeWidth(1.5);
                    if (gpxWaypoint.isGPXRouteWaypoint()) {
                        line.setStroke(Color.DARKRED);
                    } else {
                        line.setStroke(Color.DARKBLUE);
                    }

                    // bind ends of line:
                    line.startXProperty().bind(prevIcon.layoutXProperty().add(prevIcon.translateXProperty()));
                    line.startYProperty().bind(prevIcon.layoutYProperty().add(prevIcon.translateYProperty()));
                    line.endXProperty().bind(icon.layoutXProperty().add(icon.translateXProperty()));
                    line.endYProperty().bind(icon.layoutYProperty().add(icon.translateYProperty()));

                    this.getChildren().add(line);
                }

                myPoints.add(Triple.of(gpxWaypoint, icon, line));
                prevIcon = icon;
                prevWaypoint = gpxWaypoint;

                // keep track of bounding box
                // http://gamedev.stackexchange.com/questions/70077/how-to-calculate-a-bounding-rectangle-of-a-polygon
                minLat = Math.min(minLat, gpxWaypoint.getWaypoint().getLatitude());
                maxLat = Math.max(maxLat, gpxWaypoint.getWaypoint().getLatitude());
                minLon = Math.min(minLon, gpxWaypoint.getWaypoint().getLongitude());
                maxLon = Math.max(maxLon, gpxWaypoint.getWaypoint().getLongitude());
                
                count++;
            }
        }
        
        // this is our new bounding box
        myBoundingBox = new BoundingBox(minLat, minLon, maxLat-minLat, maxLon-minLon);

        this.markDirty();
    }
    
    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        selectedGPXWaypoints.clear();
        selectedGPXWaypoints.addAll(gpxWaypoints);
        this.markDirty();
    }

    public void clearSelectedGPXWaypoints() {
        selectedGPXWaypoints.clear();
        this.markDirty();
    }

    public MapPoint getCenter() {
        return new MapPoint(myBoundingBox.getMinX()+myBoundingBox.getWidth()/2, myBoundingBox.getMinY()+myBoundingBox.getHeight()/2);
    }

    public double getZoom() {
        // http://stackoverflow.com/questions/4266754/how-to-calculate-google-maps-zoom-level-for-a-bounding-box-in-java
        int zoomLevel;
        
        final double maxDiff = (myBoundingBox.getWidth() > myBoundingBox.getHeight()) ? myBoundingBox.getWidth() : myBoundingBox.getHeight();
        if (maxDiff < 360d / Math.pow(2, 20)) {
            zoomLevel = 21;
        } else {
            zoomLevel = (int) (-1d*( (Math.log(maxDiff)/Math.log(2d)) - (Math.log(360d)/Math.log(2d))) + 1d);
            if (zoomLevel < 1)
                zoomLevel = 1;
        }
        
        return zoomLevel;
    }

    @Override
    protected void layoutLayer() {
        boolean prevSelected = false;
        for (Triple<GPXWaypoint, Node, Line> triple : myPoints) {
            final GPXWaypoint point = triple.getLeft();
            final Node icon = triple.getMiddle();
            final Line line = triple.getRight();
            
            final boolean selected = selectedGPXWaypoints.contains(point);
            // first point doesn't have a line
            if (line != null) {
                if (selected && prevSelected) {
                    // if selected AND previously selected => red line
                    line.setStrokeWidth(2);
                    line.setStroke(Color.RED);
                } else {
                    line.setStrokeWidth(1);
                    line.setStroke(Color.BLUE);
                }
            }
            prevSelected = selected;
            
            final Point2D mapPoint = baseMap.getMapPoint(point.getWaypoint().getLatitude(), point.getWaypoint().getLongitude());
            icon.toFront();
            icon.setTranslateX(mapPoint.getX());
            icon.setTranslateY(mapPoint.getY());
        }
    }
}

// inspired by https://stackoverflow.com/questions/28952133/how-to-add-two-vertical-lines-with-javafx-linechart/28955561#28955561
class GPXWaypointChart<X,Y> extends AreaChart {
    private final List<Pair<GPXWaypoint, Double>> myPoints = new ArrayList<>();
    private final ObservableList<Triple<GPXWaypoint, Double, Node>> selectedGPXWaypoints;
    
    private boolean noLayout = false;

    public GPXWaypointChart() {
        super(new NumberAxis(), new NumberAxis());
        
        ((NumberAxis) getXAxis()).setLowerBound(0.0);
        ((NumberAxis) getXAxis()).setMinorTickVisible(false);
        ((NumberAxis) getXAxis()).setTickUnit(1);
        getXAxis().setAutoRanging(false);
        
        setAnimated(false);
        setCache(true);
        setCacheShape(true);
        setCacheHint(CacheHint.SPEED);

        selectedGPXWaypoints = FXCollections.observableArrayList((Triple<GPXWaypoint, Double, Node> data1) -> new Observable[]{new SimpleDoubleProperty(data1.getMiddle())});
        selectedGPXWaypoints.addListener((InvalidationListener)observable -> layoutPlotChildren());
    }
    
    public void setGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        setVisible(false);
        myPoints.clear();
        getData().clear();
        
        double distance = 0d;
        final List<XYChart.Data> dataList = new ArrayList<>();
        for (GPXWaypoint gpxWaypoint : gpxWaypoints) {
            distance += gpxWaypoint.getDistance();
            XYChart.Data data = new XYChart.Data(distance / 1000.0, gpxWaypoint.getElevation());
            // show elevation data on hover
            // https://gist.github.com/jewelsea/4681797
            data.setNode(
                new HoveredNode(String.format("Dist %.2fkm", distance / 1000.0) + "\n" + String.format("Elev %.2fm", gpxWaypoint.getElevation()))
            );
            dataList.add(data);
            myPoints.add(Pair.of(gpxWaypoint, distance));
        }
        
        // calculate scaling for ticks so their number is smaller than 25
        double tickUnit = 1.0;
        if (distance / 1000.0 > 24.9) {
            tickUnit = 2.0;
        }
        if (distance / 1000.0 > 49.9) {
            tickUnit = 5.0;
        }
        if (distance / 1000.0 > 499.9) {
            tickUnit = 50.0;
        }
        if (distance / 1000.0 > 4999.9) {
            tickUnit = 500.0;
        }
        ((NumberAxis) getXAxis()).setTickUnit(tickUnit);
        
        XYChart.Series series = new XYChart.Series();
        series.getData().addAll(dataList);
        getData().add(series);
        ((NumberAxis) getXAxis()).setUpperBound(distance / 1000.0);
    }

    public void setSelectedGPXWaypoints(final List<GPXWaypoint> gpxWaypoints) {
        noLayout = true;

        for (Triple<GPXWaypoint, Double, Node> waypoint : selectedGPXWaypoints) {
            getPlotChildren().remove(waypoint.getRight());
        }
        selectedGPXWaypoints.clear();
        
        final List<Rectangle> rectangles = new ArrayList<>();
        for (GPXWaypoint waypoint: gpxWaypoints) {
            // find matching point from myPoints
            final Pair<GPXWaypoint, Double> point = myPoints.stream()
                .filter(x -> x.getLeft().equals(waypoint))
                .findFirst().orElse(null);
            
            assert point != null;
            
            Rectangle rectangle = new Rectangle(0,0,0,0);
            rectangle.getStyleClass().add("chart-vert-rect");
            rectangles.add(rectangle);
            selectedGPXWaypoints.add(Triple.of(waypoint, point.getRight(), rectangle));
        }

        if (rectangles.size() > 0) {
            getPlotChildren().addAll(rectangles);
        }

        noLayout = false;
        layoutPlotChildren();
    }
    
    public void clearSelectedGPXWaypoints() {
        noLayout = true;
        
        for (Triple<GPXWaypoint, Double, Node> waypoint : selectedGPXWaypoints) {
            getPlotChildren().remove(waypoint.getRight());
        }
        selectedGPXWaypoints.clear();
        
        noLayout = false;
        layoutPlotChildren();
    }

    @Override
    protected void layoutPlotChildren() {
        if (noLayout) return;
        
        super.layoutPlotChildren();
        
        Pair<GPXWaypoint, Double> prevPair = null;
        boolean prevSelected = false;
        for (Pair<GPXWaypoint, Double> pair : myPoints) {
            final GPXWaypoint point = pair.getLeft();
            
            final Triple<GPXWaypoint, Double, Node> selectedPoint = selectedGPXWaypoints.stream()
                    .filter(x -> x.getLeft().equals(point))
                    .findFirst().orElse(null);
            
            if (selectedPoint != null) {
                Rectangle rect = (Rectangle) selectedPoint.getRight();
                if (prevPair != null) {
                    rect.setWidth(getXAxis().getDisplayPosition(selectedPoint.getMiddle() / 1000.0) - getXAxis().getDisplayPosition(prevPair.getRight() / 1000.0));
                } else {
                    rect.setWidth(1);
                }
                rect.setX(getXAxis().getDisplayPosition(selectedPoint.getMiddle() / 1000.0) - rect.getWidth());
                rect.setY(0d);
                rect.setHeight(getBoundsInLocal().getHeight());
            }
            
            prevSelected = (selectedPoint != null);
            prevPair = pair;
        }
    }
}
