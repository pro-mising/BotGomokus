package com.kob.backend.service.impl.ranklist;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RanklistRefreshMessage {
    private String type;
}
