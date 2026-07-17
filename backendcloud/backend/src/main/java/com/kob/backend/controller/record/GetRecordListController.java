package com.kob.backend.controller.record;

import com.alibaba.fastjson2.JSONObject;
import com.kob.backend.service.record.GetRecordListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GetRecordListController {

    @Autowired
    private GetRecordListService getRecordListService;

    @GetMapping("/record/getlist/")
    JSONObject getList(@RequestParam Map<String, String> data) {
        Integer page = Integer.parseInt(data.get("page"));
        return getRecordListService.getList(page);
    }

    @GetMapping("/record/center/list/")
    JSONObject getCenterList(@RequestParam Map<String, String> data) {
        return getRecordListService.getCenterList(data);
    }

    @GetMapping("/record/favorite/add/")
    Map<String, String> addFavorite(@RequestParam Map<String, String> data) {
        return getRecordListService.addFavorite(Integer.parseInt(data.get("record_id")));
    }

    @GetMapping("/record/favorite/remove/")
    Map<String, String> removeFavorite(@RequestParam Map<String, String> data) {
        return getRecordListService.removeFavorite(Integer.parseInt(data.get("record_id")));
    }
}
