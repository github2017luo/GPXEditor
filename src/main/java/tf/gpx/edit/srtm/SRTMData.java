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
package tf.gpx.edit.srtm;

import javafx.util.Pair;

/**
 *
 * @author Thomas
 */
public class SRTMData {
    public  enum SRTMDataType{
        SRTM1(3601, 1),
        SRTM3(1201, 3),
        INVALID(0, 0);
        
        int dataCount;
        int gridSize;
        SRTMDataType(int count, int size) {
            dataCount = count;
            gridSize = size;
        }
        public int getDataCount() {
            return dataCount;
        } 
        public int getGridSize() {
            return gridSize;
        } 
    }
    
    public class SRTMDataKey extends Pair<String, SRTMDataType> {
        public SRTMDataKey(String key, SRTMDataType value) {
            super(key, value);
        }
    }
    
    private final static double epsilon = 0.1d;

    private final String myDataFile;
    private final SRTMDataKey myDataKey;
    private final short[][] myDataValues; 
    private final int numberRows;
    private final int numberCols;

    public SRTMData(final String dataFile, final String name, final SRTMDataType type, final int numRows, final int numCols) {
        myDataFile = dataFile;
        myDataKey = new SRTMDataKey(name, type);
        myDataValues = new short[numRows][];
        numberRows = numRows;
        numberCols = numCols;
    }
    
    public SRTMDataKey getKey() {
        return myDataKey;
    }

    public short[][] getValues() {
        return myDataValues;
    }
    
    private double getValue(final int rowNum, final int colNum) {
        assert rowNum > -1;
        assert colNum > -1;

        double result = SRTMDataStore.NODATA;
        
        // check if in bounds
        if (rowNum < numberRows && colNum < numberCols) {
            // check if row already exists
            if (myDataValues[rowNum] != null) {
                result = myDataValues[rowNum][colNum];
            }
        }
        
        return result;
        
    }
    
    public void setValue(final int rowNum, final int colNum, final short value) {
        assert rowNum > -1;
        assert colNum > -1;
        
        // check if in bounds
        if (rowNum < numberRows && colNum < numberCols) {
            // check if row already exists
            if (myDataValues[rowNum] == null) {
                myDataValues[rowNum] = initDataRow();
            }

            // set value
            myDataValues[rowNum][colNum] = value;
        }
    }
    
    private short[] initDataRow() {
        short[] result = new short[numberCols];
        
        for (int i = 0; i < numberCols; i++) {
            result[i] = SRTMDataStore.NODATA;
        }
        
        return result;
    }

    public int getNumberRows() {
        return numberRows;
    }
    
    public int getNumberColumns() {
        return numberCols;
    }

    public double getValueForCoordinate(final double latitude, final double longitude, final SRTMDataStore.SRTMDataAverage avarageMode) {
        // actual calculation is the same for all SRTMData instances - so either use helper or static method
        return getValueForCoordinateStatic(latitude, longitude, avarageMode, this);
    }

    private static double getValueForCoordinateStatic(final double latitude, final double longitude, final SRTMDataStore.SRTMDataAverage avarageMode, final SRTMData data) {
        // https://gis.stackexchange.com/questions/43743/extracting-elevation-from-hgt-file
//        SRTM data are distributed in two levels: SRTM1 (for the U.S. and its territories and possessions)
//        with data sampled at one arc-second intervals in latitude and longitude, and SRTM3 (for the world)
//        sampled at three arc-seconds.
//        File names refer to the latitude and longitude of the lower left corner of the tile -
//        e.g. N37W105 has its lower left corner at 37 degrees north latitude and 105 degrees west longitude.
//        To be more exact, these coordinates refer to the geometric center of the lower left pixel,
//        which in the case of SRTM3 data will be about 90 meters in extent.
//
//        SRTM1 data are sampled at one arc-second of latitude and longitude and each file contains 3601 
//        lines and 3601 samples. The rows at the north and south edges as well as the columns at the east 
//        and west edges of each cell overlap and are identical to the edge rows and columns in the 
//        adjacent cell.
//
//        SRTM3 data are sampled at three arc-seconds and contain 1201 lines and 1201 samples with 
//        similar overlapping rows and columns. This organization also follows the DTED convention. 
//        Unlike DTED, however, 3 arc-second data are generated in each case by 3x3 averaging of the 1 
//        arc-second data - thus 9 samples are combined in each 3 arc-second data point. Since the primary 
//        error source in the elevation data has the characteristics of random noise this reduces that error 
//        by roughly a factor of three.

        double result = SRTMDataStore.NODATA;
        
        // convert lon & lat to name & check against self
        if (SRTMDataStore.getInstance().getNameForCoordinate(latitude, longitude).equals(data.getKey().getKey())) {
            // convert lon & lat to col & row
            
            // get arcsecs from lat & lon - values are passed as double!
            double latarcsecs = (latitude % 1) * 3600d;
            double lonarcsecs = (longitude % 1) * 3600d;

            // inacurate data has one value per 3 arcsecs
            latarcsecs /= data.getKey().getValue().getGridSize();
            lonarcsecs /= data.getKey().getValue().getGridSize();

            // data starts in north / east corner - naming is from south / east corner...
            final int rowNum = data.getKey().getValue().getDataCount() - 1 - (int) Math.round(latarcsecs);
            final int colNum = (int) Math.round(lonarcsecs);
            assert rowNum < data.getNumberRows();
            assert colNum < data.getNumberColumns();

            result = data.getValue(rowNum, colNum);
            
            if (SRTMDataStore.SRTMDataAverage.AVERAGE_NEIGHBOURS.equals(avarageMode)) {
                // "neighbour" can mean a few things... here its the following:
                // on a grid you can have up to 4 neighbours for a point
                // a: 1 neighbour: point is on center of grid tile => neighbour is this grid (upper left)
                // b: 2 neighbours: point is on stright line between east/west or north/south grid tiles => neighbours are those two grids (upper 2 rights)
                // c: 4 neighbours: point is somewhere else on a grid tile => neighbours are the this grid and the three "in the quadrant" of the point from grid center
                //
                // 1-1-1-1-1-------2-2-2-2-2-2-2-2-2
                // |       |       |       |       |
                // 1   a   1       2   X b 2   X   2
                // |       |       |       |       |
                // 1-1-1-1-1-------2-2-2-2-2-2-2-2-2
                // |       |       |       |       |
                // |       |       |       |       |
                // |       |       |       |       |
                // |-------4-4-4-4-4-4-4-4-4-------|
                // |       |       |       |       |
                // |       4   X   4   X   4       |
                // |       |       |       |       |
                // |-------4-4-4-4-4-4-4-4-4-------|
                // |       |     c |       |       |
                // |       4   X   4   X   4       |
                // |       |       |       |       |
                // |-------4-4-4-4-4-4-4-4-4-------|
                
                // weight is based on distance to center of tile - BUT where is the center of the tile?
                // lets have a look at row 0 / col 0:
                //
                //       |------0.5/0.5
                //       |       |
                //       |  0/0  |
                //       |       |
                //-0.5/-0.5------|
                // 
                // its center is 0/0, so it covers the region from -0.5/-0.5 to 0.5/0.5
                // the distance of a point to the center of the tile is therefore caculated from the fractional of lat/lon of arcsecs
                
                // distance to center of tile 
                final double latFractional = latarcsecs - (int) Math.round(latarcsecs);
                final double lonFractional = lonarcsecs - (int) Math.round(lonarcsecs);
                //System.out.println("latarcsecs: " + latarcsecs + ", latFractional: " + latFractional + ", lonarcsecs: " + lonarcsecs + ", lonFractional: " + lonFractional);

                // first, the grid that contains the coordinates
                double weight = 1d / distanceOnGrid(latFractional, lonFractional);

                double weightedResult = result * weight;
                double normalization = weight;
                //System.out.println("Result1: " + result + ", weight: " + weight + ", weightedResult: " + weightedResult + ", normalization: " + normalization);
                
                // what are the neighbouring cells? use them only if point is not too close to tile center
                int nextRowNum = -1;
                if (latFractional > epsilon * epsilon) {
                    nextRowNum = rowNum - (int) Math.signum(latFractional);
                }
                int nextColNum = -1;
                if (lonFractional > epsilon * epsilon) {
                    nextColNum = colNum + (int) Math.signum(lonFractional);
                }
                
                if (nextRowNum > -1 && nextRowNum < data.getNumberRows()) {
                    weight = 1d / distanceOnGrid(1d - Math.abs(latFractional), lonFractional);
                    weightedResult += data.getValue(nextRowNum, colNum) * weight;
                    normalization += weight;
                    //System.out.println("Result2: " + getValue(nextRowNum, colNum) + ", weight: " + weight + ", weightedResult: " + weightedResult + ", normalization: " + normalization);
                }
                if (nextColNum > -1 && nextColNum < data.getNumberColumns()) {
                    weight = 1d / distanceOnGrid(latFractional, 1d - Math.abs(lonFractional));
                    weightedResult += data.getValue(rowNum, nextColNum) * weight;
                    normalization += weight;
                    //System.out.println("Result3: " + getValue(rowNum, nextColNum) + ", weight: " + weight + ", weightedResult: " + weightedResult + ", normalization: " + normalization);
                }
                if ((nextRowNum > -1 && nextRowNum < data.getNumberRows()) && (nextColNum > -1 && nextColNum < data.getNumberColumns())) {
                    weight = 1d / distanceOnGrid(1d - Math.abs(latFractional), 1d - Math.abs(lonFractional));
                    weightedResult += data.getValue(nextRowNum, nextColNum) * weight;
                    normalization += weight;
                    //System.out.println("Result4: " + getValue(nextRowNum, nextColNum) + ", weight: " + weight + ", weightedResult: " + weightedResult + ", normalization: " + normalization);
                }

                result = weightedResult / normalization;
                //System.out.println("Result: " + result);
                //System.out.println("");
                
                //System.out.println("Neighbours found");
            }

            //System.out.println("latitude: " + latitude + ", longitude: " + longitude + ", latarcsecs: " + latarcsecs + ", lonarcsecs: " + lonarcsecs + ", rowNum: " + rowNum + ", colNum: " + colNum + ", result: " + result + ", result2: " + result2);
        }

        return result;
    }
    
    private static double distanceOnGrid(final double latDist, final double lonDist) {
        return Math.max(latDist*latDist + lonDist*lonDist, epsilon*epsilon);
    }
}