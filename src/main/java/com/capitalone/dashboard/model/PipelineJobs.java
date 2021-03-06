package com.capitalone.dashboard.model;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PipelineJobs {

    private List<PipelineJob> jobList = new ArrayList<>();

    public void addJob(JSONObject jsonObject) {
        String stage = (String) jsonObject.get("stage");
        Double durationInDouble = (Double) jsonObject.get("duration");
        Long duration = Math.round(durationInDouble != null ? durationInDouble : 0.0);
        long startedAt = getTime(jsonObject, "started_at");
        long finishedAt = getTime(jsonObject, "finished_at");
        JSONObject commit = (JSONObject) jsonObject.get("commit");
        String commitId = (String) commit.get("id");
        List<String> parentCommitIds = new ArrayList<>();
        Iterator iterator = ((JSONArray) commit.get("parent_ids")).iterator();
        while (iterator.hasNext()) {
            parentCommitIds.add((String)iterator.next());
        }
        jobList.add(new PipelineJob(stage, startedAt, finishedAt, duration, commitId, parentCommitIds));
    }

    public long getRelevantJobTime(List<String> buildStages) {
        return this.jobList.stream().filter(job -> buildStages
                .contains(job.getStage().toLowerCase()))
                .map(PipelineJob::getDuration)
                .mapToLong(Long::longValue).sum();
    }

    public long getEarliestStartTime(List<String> buildStages) {
        return this.jobList.stream().filter(job -> buildStages
                .contains(job.getStage().toLowerCase()))
                .map(PipelineJob::getStartedAt)
                .mapToLong(Long::longValue).min().orElse(0);
    }

    public long getLastEndTime(List<String> buildStages) {
        return this.jobList.stream().filter(job -> buildStages
                .contains(job.getStage().toLowerCase()))
                .map(PipelineJob::getFinishedAt)
                .mapToLong(Long::longValue).max().orElse(0);
    }

    public Iterable<String> getCommitIds() {
        return Stream.concat(jobList.stream().map(PipelineJob::getCommitId),
                jobList.stream().flatMap(j -> j.getParentCommitIds().stream()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private long getTime(JSONObject buildJson, String jsonField) {

        String dateToConsider = getString(buildJson, jsonField);
        if (dateToConsider != null) {
            return Instant.from(DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz")
                    .parse(getString(buildJson, jsonField))).toEpochMilli();
        } else {
            return 0L;
        }
    }

    private String getString(JSONObject json, String key) {
        return (String) json.get(key);
    }
}

class PipelineJob {
    private String stage;
    private Long startedAt;
    private Long finishedAt;
    private Long duration;
    private String commitId;
    private List<String> parentCommitIds;

    PipelineJob(String stage, Long startedAt, Long finishedAt, Long duration, String commitId, List<String> parentCommitIds) {
        this.stage = stage;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.duration = duration;
        this.commitId = commitId;
        this.parentCommitIds = parentCommitIds;
    }

    String getStage() {
        return stage;
    }

    Long getDuration() {
        return duration == null ? 0 : duration;
    }

    Long getStartedAt() {
        return startedAt;
    }

    Long getFinishedAt() {
        return finishedAt;
    }

    String getCommitId() {
        return commitId;
    }

    public List<String> getParentCommitIds() {
        return parentCommitIds;
    }
}
