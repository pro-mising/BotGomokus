package com.kob.backend.controller.user.account;

import com.alibaba.fastjson2.JSONObject;
import com.kob.backend.service.user.account.InfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class InfoController {
    @Autowired
    private InfoService infoService;

    @GetMapping("/user/account/info/")
    public Map<String,String> getInfo(){
        return infoService.getinfo();
    }

    @GetMapping("/user/account/online/")
    public Map<String,String> getOnlineStatus(){
        return infoService.getOnlineStatus();
    }

    @GetMapping("/user/account/profile/overview/")
    public JSONObject getProfileOverview() {
        return infoService.getProfileOverview();
    }
}
