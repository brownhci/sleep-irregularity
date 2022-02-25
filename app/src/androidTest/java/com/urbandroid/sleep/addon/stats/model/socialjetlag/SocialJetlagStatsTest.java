package com.urbandroid.sleep.addon.stats.model.socialjetlag;

import static org.assertj.core.api.Assertions.assertThat;

import com.urbandroid.sleep.addon.stats.model.StatRecord;
import com.urbandroid.util.ScienceUtil;

import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;

public class SocialJetlagStatsTest {

    private ChronoRecords cr = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
            //The first 3 records are in UTC timezone
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-11T01:30:10Z")),
                    Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                    1.5f, 10f, 7f),
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-12T01:00:10Z")),
                    Date.from(Instant.parse("2018-11-12T10:00:10Z")),
                    1.0f, 10f, 7f),
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-13T02:00:10Z")),
                    Date.from(Instant.parse("2018-11-13T10:00:10Z")),
                    2.0f, 10f, 6f),
            //The next 3 are in UTC+2
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-24T01:00:10Z")),
                    Date.from(Instant.parse("2018-11-24T10:00:10Z")),
                    3.0f, 12f, 7f),
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-25T02:00:10Z")),
                    Date.from(Instant.parse("2018-11-25T10:00:10Z")),
                    4.0f, 12f, 6f),
            new ChronoRecord(
                    Date.from(Instant.parse("2018-11-26T01:00:10Z")),
                    Date.from(Instant.parse("2018-11-26T09:00:10Z")),
                    3.0f, 11f, 6f),
    }));

    @Test
    public void useUtc() {
        SocialJetlagStats  stats = new SocialJetlagStats(cr, true);
        assertThat(stats.getSleepIrregularity()).isEqualTo(0.42305467f);
    }

    @Test
    public void dontUseUtc() {
        SocialJetlagStats  stats = new SocialJetlagStats(cr, false);
        assertThat(stats.getSleepIrregularity()).isEqualTo(0.7163131f);
    }

    @Test
    public void testGetRecordIrregularity() {

        SocialJetlagStats  stats = new SocialJetlagStats(cr, false);
        assertThat(CyclicFloatKt.center(stats.getRecords().getMidSleeps(), 24)).isEqualTo(6.625f);
        assertThat(ScienceUtil.avg(stats.getRecords().getLengths())).isEqualTo(6.5f);

        StatRecord record = new StatRecord(
                Date.from(Instant.parse("2018-11-11T01:30:10Z")),
                Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                TimeZone.getTimeZone("UTC"),
                0, 7.0);
        record.setTrackLengthInHours(8.5f);

        assertThat(stats.getRecordIrregularity(record)).isEqualTo( ((6.625f - 3.5f) + (8.5f - 6.5f)) / 2f );
    }

    @Test
    public void calculateSRIEdgeCases() {
        SocialJetlagStats  stats = new SocialJetlagStats(cr, false);

        // one bitset empty
        BitSet day1Interval = new BitSet(1440);
        BitSet day2Interval = new BitSet();
        day1Interval.set(0,720);
        Assert.assertEquals(1.0, stats.calculateSRI(day1Interval, day2Interval), .0001);

        // awake 24 hours for day 1, awake 0 for day 2
        BitSet interval3 = new BitSet(1440);
        BitSet interval4 = new BitSet(1440);
        interval3.set(0,1440);
        Assert.assertEquals(0, stats.calculateSRI(interval3, interval4), .0001);

        // awake 0 hours across both days
        BitSet interval5 = new BitSet(1440);
        BitSet interval6 = new BitSet(1440);
        Assert.assertEquals(1.0, stats.calculateSRI(interval5, interval6), .0001);

        // awake 24 hours both days
        BitSet interval7 = new BitSet(1440);
        BitSet interval8 = new BitSet(1440);
        interval7.set(0,1440);
        interval8.set(0,1440);
        Assert.assertEquals(1.0, stats.calculateSRI(interval7, interval8), .0001);

        //both intervals are the same
        BitSet interval9 = new BitSet(1440);
        BitSet interval10 = new BitSet(1440);
        interval9.set(0, 720);
        interval10.set(0, 720);
        Assert.assertEquals(1.0, stats.calculateSRI(interval9, interval10), .0001);
    }

    @Test
    public void testCalculateSRI() {
        BitSet day1Interval = new BitSet(1440);
        BitSet day2Interval = new BitSet(1440);
        BitSet day3Interval = new BitSet(1440);
        day1Interval.set(0,720);
        day2Interval.set(0,720);
        day3Interval.set(0,1440);

        // testing for when the intevals are the same
        SocialJetlagStats  stats = new SocialJetlagStats(cr, false);
        float sameInterval = stats.calculateSRI(day1Interval, day2Interval);
        Assert.assertEquals(1.0, sameInterval, .0001);

        // general test, one interval enclosed inside the other
        Assert.assertEquals(0.5, stats.calculateSRI(day1Interval, day3Interval), .0001);
        Assert.assertEquals(0.5, stats.calculateSRI(day2Interval, day3Interval), .0001);

        // testing when intervals are completely different
        BitSet day4Interval = new BitSet(1440);
        day4Interval.set(720, 1440);
        Assert.assertEquals(0, stats.calculateSRI(day1Interval, day4Interval), .0001);

        // testing when there is some overlap
        BitSet interval5 = new BitSet(1440);
        BitSet interval6 = new BitSet(1440);
        interval5.set(0, 720);
        interval6.set(360, 1440);
        Assert.assertEquals(0.25, stats.calculateSRI(interval5, interval6), .0001);

        // single overlap
        BitSet interval7 = new BitSet(1440);
        BitSet interval8 = new BitSet(1440);
        interval7.set(0, 2);
        interval8.set(1, 2);
        Assert.assertEquals(1 - 1.0/1440.0, stats.calculateSRI(interval7, interval8), .000001);

        // pretty regular
        BitSet interval9 = new BitSet(1440);
        BitSet interval10 = new BitSet(1440);
        interval9.set(0, 720);
        interval10.set(100, 820);
        double dayDiff = (820 - 720) + (100);
        Assert.assertEquals(1.0 - dayDiff/1440.0, stats.calculateSRI(interval9, interval10), .000001);
    }

    @Test
    public void testCalculateAvgSRIBase() {
        ChronoRecords halfDayDiff = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
                //The first 3 records are in UTC timezone
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T01:30:10Z")),
                        Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                        1.5f, 10f, 7f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-12T01:00:10Z")),
                        Date.from(Instant.parse("2018-11-12T10:00:10Z")),
                        1.0f, 10f, 7f),}));
        SocialJetlagStats  stats = new SocialJetlagStats(halfDayDiff, false);
        List<Interval> recordsAsIntervals = stats.convertChronoRecordsToTimeIntervals(halfDayDiff);
        TreeMap<String, BitSet> dayBitMap = stats.createSleepStateByDateMap(recordsAsIntervals);
        float avgSRI = stats.calculateAverageSRI(dayBitMap);
        Assert.assertEquals(1.0-30.0/1440.0, avgSRI, 0.001);
    }

    @Test
    public void overLappingDays() {
        ChronoRecords halfDayDiff = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
                //The first 3 records are in UTC timezone
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T00:00:10Z")),
                        Date.from(Instant.parse("2018-11-11T06:00:10Z")),
                        10f, 6f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                        Date.from(Instant.parse("2018-11-12T06:00:10Z")),
                        10f, 6f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-12T10:30:10Z")),
                        Date.from(Instant.parse("2018-11-13T06:30:10Z")),
                        10.5f, 6.5f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-13T10:00:10Z")),
                        Date.from(Instant.parse("2018-11-13T23:59:10Z")),
                        10f, 6f, 8f)}));
        SocialJetlagStats  stats = new SocialJetlagStats(halfDayDiff, false);
        List<Interval> recordsAsIntervals = stats.convertChronoRecordsToTimeIntervals(halfDayDiff);
        TreeMap<String, BitSet> dayBitMap = stats.createSleepStateByDateMap(recordsAsIntervals);
        float avgSRI = stats.calculateAverageSRI(dayBitMap);
        Assert.assertEquals(1.0-45.0/1440.0, avgSRI, 0.001);
    }

    @Test
    public void testSkipDays() {
        ChronoRecords halfDayDiff = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
                //The first 3 records are in UTC timezone
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T00:00:10Z")),
                        Date.from(Instant.parse("2018-11-11T06:00:10Z")),
                        10f, 6f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-12T00:30:10Z")),
                        Date.from(Instant.parse("2018-11-12T06:30:10Z")),
                        10f, 6f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-14T00:00:10Z")),
                        Date.from(Instant.parse("2018-11-14T06:00:10Z")),
                        10f, 6f, 8f),
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-15T00:30:10Z")),
                        Date.from(Instant.parse("2018-11-15T06:30:10Z")),
                        10f, 6f, 8f),
        }));
        SocialJetlagStats  stats = new SocialJetlagStats(halfDayDiff, false);
        List<Interval> recordsAsIntervals = stats.convertChronoRecordsToTimeIntervals(halfDayDiff);
        TreeMap<String, BitSet> dayBitMap = stats.createSleepStateByDateMap(recordsAsIntervals);
        float avgSRI = stats.calculateAverageSRI(dayBitMap);
        Assert.assertEquals(1.0-60.0/1440.0, avgSRI, 0.001);
    }

    @Test // does not work when only one ChronoRecord is given
    public void testOneValMap() {
        ChronoRecords oneDayRecord = new ChronoRecords(Arrays.asList(new ChronoRecord[]{
                //The first 3 records are in UTC timezone
                new ChronoRecord(
                        Date.from(Instant.parse("2018-11-11T01:30:10Z")),
                        Date.from(Instant.parse("2018-11-11T10:00:10Z")),
                        1.5f, 10f, 7f)}));
        SocialJetlagStats  stats = new SocialJetlagStats(oneDayRecord, false);
        List<Interval> recordsAsIntervals = stats.convertChronoRecordsToTimeIntervals(oneDayRecord);
        TreeMap<String, BitSet> dayBitMap = stats.createSleepStateByDateMap(recordsAsIntervals);
        float avgSRI = stats.calculateAverageSRI(dayBitMap);
        Assert.assertEquals(-1.0, avgSRI, 0.001); // should be -1 when only one chronorecord is given
    }

    @Test
    public void testEmpyValMap() {
        ChronoRecords emptyRecord = new ChronoRecords(Arrays.asList(new ChronoRecord[]{}));
        SocialJetlagStats  stats = new SocialJetlagStats(emptyRecord, false);
        List<Interval> recordsAsIntervals = stats.convertChronoRecordsToTimeIntervals(emptyRecord);
        TreeMap<String, BitSet> dayBitMap = stats.createSleepStateByDateMap(recordsAsIntervals);
        float avgSRI = stats.calculateAverageSRI(dayBitMap);
        Assert.assertEquals(-1.0, avgSRI, 0.001); // should be -1 when only one chronorecord is given
    }
}
