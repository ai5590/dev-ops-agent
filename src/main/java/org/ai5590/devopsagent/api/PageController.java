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
}
