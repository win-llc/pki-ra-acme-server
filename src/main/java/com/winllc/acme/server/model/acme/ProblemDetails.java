package com.winllc.acme.server.model.acme;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.winllc.acme.server.contants.ProblemType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//RFC7807
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetails {
    private String type;
    private String title;
    private Integer status;
    private String detail;
    private String instance;
    //Section 6.7.1
    private ProblemDetails[] subproblems;

    public ProblemDetails(ProblemType problemType, Integer status){
        this.type = problemType.getValue();
        this.status = status;
    }

    public ProblemDetails(ProblemType problemType){
        this.type = problemType.getValue();
    }

    private ProblemDetails(){}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public ProblemDetails[] getSubproblems() {
        if(subproblems == null) subproblems = new ProblemDetails[0];
        return subproblems;
    }

    public void setSubproblems(ProblemDetails[] subproblems) {
        this.subproblems = subproblems;
    }

    public void addSubproblem(ProblemDetails problemDetails){
        if(subproblems == null) subproblems = new ProblemDetails[0];
        List<ProblemDetails> list = Arrays.asList(subproblems);
        List<ProblemDetails> temp = new ArrayList<>(list);
        temp.add(problemDetails);
        subproblems = temp.toArray(new ProblemDetails[0]);
    }

    @Override
    public String toString() {
        return "ProblemDetails{" +
                "type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", detail='" + detail + '\'' +
                ", instance='" + instance + '\'' +
                ", subproblems=" + Arrays.toString(subproblems) +
                '}';
    }
}
