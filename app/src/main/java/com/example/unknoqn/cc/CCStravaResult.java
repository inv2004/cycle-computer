package com.example.unknoqn.cc;

import com.sweetzpot.stravazpot.segment.model.Segment;

import java.util.List;

/**
 * Created by unknown on 7/23/2017.
 */

public interface CCStravaResult {
    void onStravaResult(String[] msg);

    void onStravaResultSegmentList(List<Segment> result);

    void onStravaResultSegment(Segment result);
}
