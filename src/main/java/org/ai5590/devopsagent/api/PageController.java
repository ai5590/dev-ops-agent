package org.ai5590.devopsagent.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/chat")
    public String chat() {
        return "forward:/chat.html";
    }

    @GetMapping("/help")
    public String help() {
        return "forward:/help.html";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/login.html";
    }

    @GetMapping("/settings")
    public String settings() {
        return "forward:/settings.html";
    }

    @GetMapping("/servers")
    public String servers() {
        return "forward:/servers.html";
    }
}
