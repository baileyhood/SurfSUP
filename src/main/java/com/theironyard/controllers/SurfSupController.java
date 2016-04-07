package com.theironyard.controllers;

import com.theironyard.entities.User;
import com.theironyard.services.UserRepository;
import com.theironyard.utilities.PasswordStorage;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;


/**
 * Created by keatonfoster on 4/6/16.
 */
@RestController
public class SurfSupController {

    @Autowired
    UserRepository users;

    Server dbui;

    @PostConstruct
    public void construct() throws SQLException, SQLException {
        dbui = Server.createWebServer().start();
    }

    @PreDestroy
    public void destroy() {
        dbui.stop();
    }

    // CREATE USER ROUTE /user
    @RequestMapping(path = "/user", method = RequestMethod.POST)
    public User createUser (@RequestBody User user, HttpSession session) throws Exception {
        if (users.findByUsername(user.getUsername()) == null) {
            user.setPassword(PasswordStorage.createHash(user.getPassword()));
            session.setAttribute("username", user.getUsername());
            users.save(user);
            return user;
        }
        else {
            throw new Exception("Username already taken");
        }

    }

    // LOGIN ROUTE /login
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public User login (@RequestBody User user, HttpSession session) throws Exception {
        User existing = users.findByUsername(user.getUsername());
        if (existing != null) {

            //SUCCESS SCENARIO
            if (PasswordStorage.verifyPassword(user.getPassword(), existing.getPassword())) {
                session.setAttribute("username", user.getUsername());
                return user;

            //PASSWORD FAIL SCENARIO
            } else if (!PasswordStorage.verifyPassword(user.getPassword(), existing.getPassword())) {
                throw new Exception("Password do not match");
            }
        }
        return null;
    }

    // LOGOUT ROUTE /logout
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public void logout (HttpSession session) {
        session.invalidate();
    }

    // UPLOAD PROFILE PICTURE /upload
    @RequestMapping(path = "/upload", method = RequestMethod.PUT)
    public void addProfile (HttpSession session) {

    }

}
