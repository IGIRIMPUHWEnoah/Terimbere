package com.terimbere.budget.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerConfigResponse {
    private Integer hour;
    private Integer minute;
    private String cronExpression;
    private String displayTime;
}
