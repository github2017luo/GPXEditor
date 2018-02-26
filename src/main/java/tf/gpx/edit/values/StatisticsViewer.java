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
package tf.gpx.edit.values;

import java.text.Format;
import java.util.Date;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tf.gpx.edit.helper.GPXFile;
import tf.gpx.edit.helper.GPXLineItem;
import tf.gpx.edit.helper.GPXWaypoint;
import tf.gpx.edit.main.GPXEditorManager;

/**
 *
 * @author thomas
 */
public class StatisticsViewer {
    // this is a singleton for everyones use
    // http://www.javaworld.com/article/2073352/core-java/simply-singleton.html
    private final static StatisticsViewer INSTANCE = new StatisticsViewer();
    
    private final ObservableList<StatisticValue> statisticsList = FXCollections.observableArrayList();
    
    // for what do we calc statistics
    private static enum StatisticData {
        // overall
        Start("Start", "", Date.class, GPXLineItem.DATE_FORMAT),
        End("End", "", Date.class, GPXLineItem.DATE_FORMAT),
        Count("Count Waypoints", "", Integer.class, GPXLineItem.COUNT_FORMAT),
        
        Break1("", "", String.class, null),

        // duration
        Duration("Duration", "hhh:mm:ss", String.class, null),
        DurationAscent("Duration ascending", "hhh:mm:ss", String.class, null),
        DurationDescent("Duration descending", "hhh:mm:ss", String.class, null),
        // DurationTravel("Duration of Travel", "HH:MM:SS", String.class, GPXLineItem.TIME_FORMAT),
        // DurationBreaks("Duration of Breaks (Breaks > 3 min)", "HH:MM:SS", String.class, GPXLineItem.TIME_FORMAT),

        Break2("", "", String.class, null),

        // length
        Length("Length", "km", Double.class, GPXLineItem.DOUBLE_FORMAT_3),
        LengthAscent("Length ascending", "km", Double.class, GPXLineItem.DOUBLE_FORMAT_3),
        LengthDescent("Length descending", "km", Double.class, GPXLineItem.DOUBLE_FORMAT_3),

        Break3("", "", String.class, null),

        // height & slope
        StartHeight("Initial Height", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        EndHeight("Final Height", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        MinHeight("Min. Height", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        MaxHeight("Max. Height", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgHeight("Avg. Height", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        CumulativeAscent("Total Ascent", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        CumulativeDescent("Total Descent", "m", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        MaxSlopeAscent("Max. Slope ascending", "%", Double.class, GPXLineItem.SLOPE_FORMAT),
        MaxSlopeDescent("Max. Slope descending", "%", Double.class, GPXLineItem.SLOPE_FORMAT),
        AvgSlopeAscent("Avg. Slope ascending", "%", Double.class, GPXLineItem.SLOPE_FORMAT),
        AvgSlopeDescent("Avg. Slope descending", "%", Double.class, GPXLineItem.SLOPE_FORMAT),

        Break4("", "", String.class, null),

        // speed
        MaxSpeed("Max. Speed", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgSpeeed("Avg. Speed", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        MaxSpeedAscent("Max. Speed ascending", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgSpeeedAscent("Avg. Speed ascending", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        MaxSpeedDescent("Max. Speed descending", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        AvgSpeeedDescent("Avg. Speed descending", "km/h", Double.class, GPXLineItem.DOUBLE_FORMAT_2),
        ;
        
        private final String description;
        private final String unit;
        private final Class<?> type;
        private final Format format;
        
        StatisticData(final String desc, final String un, final Class<?> ty, final Format form) {
            description = desc;
            unit = un;
            type = ty;
            format = form;
        }
        
        public String getDescription() {
            return description;
        }

        public String getUnit() {
            return unit;
        }
        
        public Class<?> getType(){
            return type;
        }
        
        public Format getFormat(){
            return format;
        }
    }
    
    // UI elements used in various methods need to be class-wide
    final Stage statisticsStage = new Stage();
    final TableView<StatisticValue> table = new TableView<>();
    
    private GPXFile myGPXFile;

    private StatisticsViewer() {
        // Exists only to defeat instantiation.
        
        initViewer();
    }

    public static StatisticsViewer getInstance() {
        return INSTANCE;
    }
    
    private void initViewer() {
        // add one item to list for each enum value
        for (StatisticData data : StatisticData.values()) {
            statisticsList.add(new StatisticValue(data));
        }

        // create new scene
        statisticsStage.setTitle("Statistics");
        statisticsStage.initModality(Modality.WINDOW_MODAL);

        final GridPane gridPane = new GridPane();

        // data will be shown in a table
        table.setEditable(false);
        table.setSelectionModel(null);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("stat-table");
        
        // Create column Description, Value, Unit
        TableColumn<StatisticValue, String> descCol //
                = new TableColumn<>("Observable");
        descCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<StatisticValue, String> p) -> new SimpleStringProperty(p.getValue().getDescription()));
        descCol.setSortable(false);
        TableColumn<StatisticValue, String> valueCol //
                = new TableColumn<>("Value");
        valueCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<StatisticValue, String> p) -> new SimpleStringProperty(p.getValue().getStringValue()));
        valueCol.setSortable(false);
        valueCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<StatisticValue, String> unitCol //
                = new TableColumn<>("Unit");
        unitCol.setCellValueFactory(
                (TableColumn.CellDataFeatures<StatisticValue, String> p) -> new SimpleStringProperty(p.getValue().getUnit()));
        unitCol.setSortable(false);
        
        table.getColumns().addAll(descCol, valueCol, unitCol);
        
        table.setItems(statisticsList);
        
        gridPane.add(table, 0, 0);
        GridPane.setHgrow(table, Priority.ALWAYS);
        GridPane.setVgrow(table, Priority.ALWAYS);
      
        statisticsStage.setScene(new Scene(gridPane));
        statisticsStage.getScene().getStylesheets().add(GPXEditorManager.class.getResource("/GPXEditor.css").toExternalForm());
        statisticsStage.setWidth(400);
        statisticsStage.setHeight(780);
        statisticsStage.setResizable(false);
    }
    
    public boolean showStatistics(final GPXFile gpxFile) {
        assert gpxFile != null;
        
        if (statisticsStage.isShowing()) {
            statisticsStage.close();
        }
        
        myGPXFile = gpxFile;
        // initialize the whole thing...
        initStatisticsViewer();
        
        statisticsStage.show();
        statisticsStage.setWidth(table.getWidth());
        statisticsStage.setHeight(table.getHeight());
        statisticsStage.hide();

        statisticsStage.showAndWait();
                
        return true;
    }
    
    private void initStatisticsViewer() {
        // reset all previous values
        for (StatisticValue value : statisticsList) {
            value.setValue(null);
        }
        statisticsList.get(StatisticData.Break1.ordinal()).setValue("");
        statisticsList.get(StatisticData.Break2.ordinal()).setValue("");
        statisticsList.get(StatisticData.Break3.ordinal()).setValue("");
        statisticsList.get(StatisticData.Break4.ordinal()).setValue("");
        
        final List<GPXWaypoint> gpxWaypoints = myGPXFile.getGPXWaypoints(GPXLineItem.GPXLineItemType.GPXTrack);
        
        // set values that don't need calculation
        statisticsList.get(StatisticData.Start.ordinal()).setValue(gpxWaypoints.get(0).getDate());
        statisticsList.get(StatisticData.End.ordinal()).setValue(gpxWaypoints.get(gpxWaypoints.size()-1).getDate());
        statisticsList.get(StatisticData.Count.ordinal()).setValue(gpxWaypoints.size());
        
        // format duration as in getDurationAsString
        statisticsList.get(StatisticData.Duration.ordinal()).setValue(myGPXFile.getDurationAsString());
        statisticsList.get(StatisticData.Length.ordinal()).setValue(myGPXFile.getLength()/1000d);
        
        statisticsList.get(StatisticData.StartHeight.ordinal()).setValue(gpxWaypoints.get(0).getElevation());
        statisticsList.get(StatisticData.EndHeight.ordinal()).setValue(gpxWaypoints.get(gpxWaypoints.size()-1).getElevation());
        statisticsList.get(StatisticData.MinHeight.ordinal()).setValue(myGPXFile.getMinHeight());
        statisticsList.get(StatisticData.MaxHeight.ordinal()).setValue(myGPXFile.getMaxHeight());

        statisticsList.get(StatisticData.CumulativeAscent.ordinal()).setValue(myGPXFile.getCumulativeAscent());
        statisticsList.get(StatisticData.CumulativeDescent.ordinal()).setValue(myGPXFile.getCumulativeDescent());
        
        statisticsList.get(StatisticData.AvgSpeeed.ordinal()).setValue(myGPXFile.getLength()/myGPXFile.getDuration()*1000d*3.6d);

        double lengthAsc = 0.0;
        double lengthDesc = 0.0;
        long durationAsc = 0;
        long durationDesc = 0;
        
        double avgHeight = 0.0;
        double maxSlopeAsc = 0.0;
        double maxSlopeDesc = 0.0;
        double avgSlopeAsc = 0.0;
        double avgSlopeDesc = 0.0;
        
        double maxSpeed = 0.0;
        double maxSpeedAsc = 0.0;
        double maxSpeedDesc = 0.0;

        // walk through waypoints and calculate the remaining values...
        for (GPXWaypoint waypoint : gpxWaypoints) {
            avgHeight += waypoint.getElevation();
            maxSpeed = Math.max(maxSpeed, waypoint.getSpeed());
            
            final double heightDiff = waypoint.getElevationDiff();
            final double slope = waypoint.getSlope();
            final double speed = waypoint.getSpeed();
            
            if (heightDiff > 0.0) {
                lengthAsc += waypoint.getDistance();
                durationAsc += waypoint.getDuration();
                maxSlopeAsc = Math.max(maxSlopeAsc, slope);
                avgSlopeAsc += slope;
                maxSpeedAsc = Math.max(maxSpeedAsc, speed);
            } else {
                lengthDesc += waypoint.getDistance();
                durationDesc += waypoint.getDuration();
                maxSlopeDesc = Math.min(maxSlopeDesc, slope);
                avgSlopeDesc += slope;
                maxSpeedDesc = Math.max(maxSpeedDesc, speed);
            }
        }
        
        // average values
        avgHeight /= gpxWaypoints.size();
        avgSlopeAsc /= gpxWaypoints.size();
        avgSlopeDesc /= gpxWaypoints.size();

        
        statisticsList.get(StatisticData.LengthAscent.ordinal()).setValue(lengthAsc/1000d);
        statisticsList.get(StatisticData.LengthDescent.ordinal()).setValue(lengthDesc/1000d);
        
        statisticsList.get(StatisticData.DurationAscent.ordinal()).setValue(GPXLineItem.formatDurationAsString(durationAsc));
        statisticsList.get(StatisticData.DurationDescent.ordinal()).setValue(GPXLineItem.formatDurationAsString(durationDesc));

        statisticsList.get(StatisticData.AvgHeight.ordinal()).setValue(avgHeight);
        statisticsList.get(StatisticData.MaxSlopeAscent.ordinal()).setValue(maxSlopeAsc);
        statisticsList.get(StatisticData.MaxSlopeDescent.ordinal()).setValue(maxSlopeDesc);
        statisticsList.get(StatisticData.AvgSlopeAscent.ordinal()).setValue(avgSlopeAsc);
        statisticsList.get(StatisticData.AvgSlopeDescent.ordinal()).setValue(avgSlopeDesc);
        
        statisticsList.get(StatisticData.MaxSpeed.ordinal()).setValue(maxSpeed);
        statisticsList.get(StatisticData.MaxSpeedAscent.ordinal()).setValue(maxSpeedAsc);
        statisticsList.get(StatisticData.MaxSpeedDescent.ordinal()).setValue(maxSpeedDesc);
        statisticsList.get(StatisticData.AvgSpeeedAscent.ordinal()).setValue(lengthAsc/durationAsc*1000d*3.6d);
        statisticsList.get(StatisticData.AvgSpeeedDescent.ordinal()).setValue(lengthDesc/durationDesc*1000d*3.6d);

        table.refresh();
    }
    
    private class StatisticValue<T> {
        private final StatisticData myData;
        private T myValue = null;
        
        StatisticValue (final StatisticData data) {
            myData = data;
        }
        
        private T getValue() {
            return myValue;
        }
        
        private void setValue(final T value) {
            myValue = value;
        }
        
        private String getDescription() {
            return myData.getDescription();
        }
        
        private String getUnit() {
            return myData.getUnit();
        }

        private String getStringValue() {
            if (myValue == null || (myValue instanceof Double && Double.isInfinite((Double) myValue))) {
                return "---";
            } else {
                if (myData.getFormat() != null) {
                    return myData.getFormat().format(myValue);
                } else {
                    return myValue.toString();
                }
            }
        }
    }
}
