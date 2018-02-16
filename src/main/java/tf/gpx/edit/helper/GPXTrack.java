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

import com.hs.gpxparser.modal.Track;
import com.hs.gpxparser.modal.TrackSegment;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 *
 * @author Thomas
 */
public class GPXTrack extends GPXMeasurable {
    private GPXFile myGPXFile;
    private Track myTrack;
    private List<GPXTrackSegment> myGPXTrackSegments = new ArrayList<>();
    
    private GPXTrack() {
        super(GPXLineItemType.GPXTrack);
    }
    
    public GPXTrack(final GPXFile gpxFile, final Track track) {
        super(GPXLineItemType.GPXTrack);
        
        myGPXFile = gpxFile;
        myTrack = track;
        
        // TFE, 20180203: track without tracksegments is valid!
        if (myTrack.getTrackSegments() != null) {
            for (TrackSegment segment : myTrack.getTrackSegments()) {
                myGPXTrackSegments.add(new GPXTrackSegment(this, segment, myGPXTrackSegments.size() + 1));
            }
            assert (myGPXTrackSegments.size() == myTrack.getTrackSegments().size());
        }
    }

    protected Track getTrack() {
        return myTrack;
    }
    
    @Override
    public Integer getNumber() {
        return myTrack.getNumber();
    }

    @Override
    public void setNumber(Integer number) {
        myTrack.setNumber(number);
        setHasUnsavedChanges();
    }

    public Comparator<GPXTrack> getComparator() {
        return (GPXTrack a, GPXTrack b) -> a.getNumber() - b.getNumber();
    }
    
    @Override
    public String getName() {
        return myTrack.getName();
    }

    @Override
    public void setName(final String name) {
        myTrack.setName(name);
        setHasUnsavedChanges();
    }

    @Override
    public GPXLineItem getParent() {
        return myGPXFile;
    }
    
    @Override
    public void setParent(final GPXLineItem parent) {
        assert GPXLineItem.GPXLineItemType.GPXFile.equals(parent.getType());
        
        myGPXFile = (GPXFile) parent;
        setHasUnsavedChanges();
    }

    @Override
    public List<GPXLineItem> getChildren() {
        return new ArrayList<>(myGPXTrackSegments);
    }
    
    @Override
    public void setChildren(final List<GPXLineItem> children) {
        setGPXTrackSegments(castChildren(GPXTrackSegment.class, children));
    }
    
    public void setGPXTrackSegments(final List<GPXTrackSegment> gpxTrackSegments) {
        //System.out.println("setGPXTrackSegments: " + getName() + ", " + gpxTrackSegments.size());
        myGPXTrackSegments = gpxTrackSegments;
        
        AtomicInteger counter = new AtomicInteger(0);
        final List<TrackSegment> trackSegments = gpxTrackSegments.stream().
                map((GPXTrackSegment child) -> {
                    child.setNumber(counter.getAndIncrement());
                    return child.getTrackSegment();
                }).collect(Collectors.toList());
        myTrack.setTrackSegments(new ArrayList<>(trackSegments));
        
        setHasUnsavedChanges();
    }

    @Override
    public GPXLineItemType getType() {
        return GPXLineItemType.GPXTrack;
    }
    
    @Override
    public List<GPXMeasurable> getGPXMeasurables() {
        return new ArrayList<>(myGPXTrackSegments);
    }
    
    @Override
    public Date getDate() {
        return getStartTime();
    }

    @Override
    public GPXFile getGPXFile() {
        return myGPXFile;
    }

    @Override
    public List<GPXTrack> getGPXTracks() {
        List<GPXTrack> result = new ArrayList<>();
        result.add(this);
        return result;
    }

    @Override
    public List<GPXTrackSegment> getGPXTrackSegments() {
        // return copy of list
        return myGPXTrackSegments.stream().collect(Collectors.toList());
    }

    @Override
    public List<GPXWaypoint> getGPXWaypoints(final GPXLineItemType itemType) {
        // iterate over my segments
        List<GPXWaypoint> result = new ArrayList<>();
        if (itemType == null || itemType.equals(GPXLineItemType.GPXTrack) || itemType.equals(GPXLineItemType.GPXTrackSegment)) {
            for (GPXTrackSegment trackSegment : myGPXTrackSegments) {
                result.addAll(trackSegment.getGPXWaypoints(itemType));
            }
        }
        return result;
    }

    @Override
    public List<GPXRoute> getGPXRoutes() {
        List<GPXRoute> result = new ArrayList<>();
        return result;
    }

    @Override
    protected void visitMe(final IGPXLineItemVisitor visitor) {
        visitor.visitGPXTrack(this);
    }
}
