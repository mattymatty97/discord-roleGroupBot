package com.roleGroup;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RESTController {

    @RequestMapping("/")
    public String main(){
        return "Base Page, try /test and add a name to get method";
    }

    @RequestMapping("/test")
    public String test(@RequestParam(value="name", defaultValue="World") String name){
        return "Hello " + name;
    }
}
