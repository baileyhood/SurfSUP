package com.theironyard.controllers;

import com.theironyard.entities.Friend;
import com.theironyard.entities.Join;
import com.theironyard.entities.Sesh;
import com.theironyard.entities.User;
import com.theironyard.services.FriendRepository;
import com.theironyard.services.JoinRepository;
import com.theironyard.services.SeshRepository;
import com.theironyard.services.UserRepository;
import com.theironyard.utilities.PasswordStorage;
import org.h2.tools.Server;
import org.omg.CORBA.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by keatonfoster on 4/6/16.
 */
@RestController
public class SurfSupController {

    static final String IOP_URL = "http://magicseaweed.com/api/05b02278d73272e0e716626de5b875e4/forecast/?spot_id=760";

    @Autowired
    UserRepository users;

    @Autowired
    SeshRepository seshs;

    @Autowired
    JoinRepository joins;

    @Autowired
    FriendRepository friends;

    Server dbui;

    @PostConstruct
    public void construct() throws SQLException, SQLException {
        dbui = Server.createWebServer().start();
    }

    @PreDestroy
    public void destroy() {
        dbui.stop();
    }

    // CREATES A USER
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

    // LOGIN
    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public User login (@RequestBody User user, HttpSession session) throws Exception {
        if (session.getAttribute("username") == null) {
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
            } else if (existing == null) {
                throw new Exception("Username does not exist in database");
            }
        }
        return null;
    }

    // CREATE A SESH
    @RequestMapping(path = "/sesh", method = RequestMethod.POST)
    public Sesh addSesh (@RequestBody Sesh sesh, HttpSession session) {
        User user = users.findByUsername((String) session.getAttribute("username"));
        sesh.setUser(user);
        seshs.save(sesh);

        //joins user and sesh in Joins table
        Join join = new Join(user, sesh);
        joins.save(join);
        return sesh;
    }

    // UPLOAD PROFILE PICTURE (IF ALREADY LOGGED IN)
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public void addProfile (@RequestBody MultipartFile photo, HttpSession session) throws IOException {
        User existing = users.findByUsername((String) session.getAttribute("username"));

        // store photo file name in db
        File dir = new File("public/profile");
        dir.mkdirs();
        File photoFile = File.createTempFile("image", photo.getOriginalFilename(), dir);
        FileOutputStream fos = new FileOutputStream(photoFile);
        fos.write(photo.getBytes());
        existing.setPhotoFileName(photoFile.getName());

        users.save(existing);
    }

    /**
     * This will create a Friend object (User requester, User responder). Does not set the parameter
     * isAccepted (this boolean is only changed when the friend request is accepted). The @RequestBody
     * String username refers to the username of the approver.
     * @param session
     * @param username
     * @throws Exception
     */
    //SEND FRIEND REQUEST
    @RequestMapping(path = "/friend", method = RequestMethod.POST)
    public void createFriend (HttpSession session, @RequestBody String username) throws Exception {
        User requester = users.findByUsername((String) session.getAttribute("username"));
        User approver = users.findByUsername(username);
        Friend friend = new Friend (requester, approver);
        if (friends.findFirstByRequesterAndApprover(requester, approver) == null){
            friends.save(friend);
        } else {
            throw new Exception("Friendship already requested");
        }
    }

    /**
     * This will create the reciprocal of the Friend object created by method: createFriend.
     * This created object will have its boolean parameter, isAccepted, set to TRUE.
     * @param session
     * @param username
     * @throws Exception
     */
    //ACCEPTS FRIEND REQUEST
    @RequestMapping(path = "/friend/friend", method = RequestMethod.POST)
    public void acceptFriend (HttpSession session, @RequestBody String username) throws Exception {
        User requester = users.findByUsername((String) session.getAttribute("username"));
        User approver = users.findByUsername(username);
        Friend friend = new Friend (requester, approver);
        if (friends.findFirstByRequesterAndApprover(requester, approver) == null){
            friend.setIsApproved(true);
            friends.save(friend);
        } else {
            throw new Exception("Friendship already requested");
        }
    }

    //INVITE FRIENDS TO JOIN A SESH (ID = USER BEING INVITED'S ID)
    @RequestMapping(path = "/join/{userId}/{seshId}", method = RequestMethod.POST)
    public void inviteFriendToJoin (@PathVariable("userId") int userId, @PathVariable("seshId") int seshId) {
        User invitedUser = users.findOne(userId);
        Sesh seshToJoin = seshs.findOne(seshId);
        Join join = new Join(invitedUser, seshToJoin);
        joins.save(join);
    }

    //JOIN A SESH (ID = SESH ID)
    @RequestMapping(path = "/join/{id}", method = RequestMethod.POST)
    public void joinSesh (@PathVariable("id") int seshId, HttpSession session) throws Exception {
        User user = users.findByUsername((String) session.getAttribute("username"));
        Sesh sesh = seshs.findOne(seshId);
        Join join = new Join (user, sesh);
        if (joins.findFirstByUserAndSesh(user, sesh) == null) {
            joins.save(join);
        }
        else {
            throw new Exception ("User already joined sesh");
        }
    }

    //WEATHER AT IOP
    @RequestMapping(path = "/weather", method = RequestMethod.GET)
    public List weather () {
        RestTemplate query = new RestTemplate();
        List result = query.getForObject(IOP_URL, List.class);
        if (result != null) {
            return result;
        }
        return null;
    }

    // CURRENT USER USERNAME
    @RequestMapping(path = "/currentUsername", method = RequestMethod.GET)
    public String loggedInUsername (HttpSession session) {
        String username = (String) session.getAttribute("username");
        return username;
    }

    //RETURNS CURRENT USER
    @RequestMapping(path = "/currentUser", method = RequestMethod.GET)
    public User loggedInUser (HttpSession session) {
        User user = users.findByUsername((String) session.getAttribute("username"));
        return user;
    }

    // LOGOUT
    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public void logout (HttpSession session) {
        session.invalidate();
    }

    // DISPLAY ALL SESHS (PUBLIC SESH LIST. THIS WOULD BE HUGE,
    // AND I DONT KNOW WHY YOUD WANT TO USE IT, BUT HERE IT IS IN CASE)
    @RequestMapping(path = "/sesh", method = RequestMethod.GET)
    public List<Sesh> displayAllSesh () {
        return (List<Sesh>) seshs.findAll();
    }

    //DISPLAY SESHS BY USER
    @RequestMapping(path = "/user/{id}/sesh", method = RequestMethod.GET)
    public List<Sesh> displaySeshByUser (@PathVariable("id") int id) {
        User user = users.findOne(id);
        List<Sesh> list = seshs.findAllByUser(user);
        return list;
    }

    //DISPLAY CURRENT USERS SESHS
    @RequestMapping(path = "currentUser/{id}", method = RequestMethod.GET)
    public List<Sesh> currentUsersSeshs (HttpSession session) {
        User user = users.findByUsername((String) session.getAttribute("username"));
        List<Sesh> userSeshs = seshs.findAllByUser(user);
        return userSeshs;
    }

    //DISPLAY SESHS BY THE CURRENT USER AND HIS/HER FRIENDS
    @RequestMapping(path = "/user/friend/sesh", method = RequestMethod.GET)
    public List<Sesh> displayUserAndFriendsSeshs (HttpSession session) {
        User loggedIn = users.findByUsername((String) session.getAttribute("username"));
        List <Sesh> usersSeshs = seshs.findAllByUser(loggedIn);
        List <Sesh> friendsSeshs = new ArrayList<>();
        // friendsSesh is to be returned product

        List<Friend> allList = friends.findAllByRequester(loggedIn);
        allList.addAll(friends.findAllByApprover(loggedIn));
        //creates a list of friend objects that contain the current user

        //Credit Alex Hughes for Parallel Stream help
        ArrayList<User> friendsList = allList.parallelStream()
                .filter(Friend::getIsApproved)
                .map(friend -> {
                    if (friend.getRequester().getId() == loggedIn.getId()) {
                        return friend.getApprover();
                    }
                    else if (friend.getApprover().getId() == loggedIn.getId()) {
                        return friend.getRequester();
                    }
                    else return null;
                })
                .collect(Collectors.toCollection(ArrayList<User>::new));

        for (User user : friendsList) {
            friendsSeshs.addAll(seshs.findAllByUser(user));
        }
        friendsSeshs.addAll(usersSeshs);
        return friendsSeshs;
    }

    //DISPLAY ALL USERS
    @RequestMapping(path = "/user", method = RequestMethod.GET)
    public List<User> displayUser (HttpSession session) {
        List<User> userList = (List<User>) users.findAll();
        User user = users.findByUsername((String) session.getAttribute("username"));
        userList.remove(user);
        return userList;
    }

    //DISPLAY FRIENDS LIST
    @RequestMapping(path = "/friend", method = RequestMethod.GET)
    public List<User> friendList (HttpSession session) {
        User user = users.findByUsername((String) session.getAttribute("username"));
        List<Friend> allList = friends.findAllByRequester(user);
        allList.addAll(friends.findAllByApprover(user));
        //creates a list of friend objects that contain the current user

        //Credit Alex Hughes for Parallel Stream help
        ArrayList<User> friendsList = allList.parallelStream()
                .filter(Friend::getIsApproved)
                .map(friend -> {
                    if (friend.getRequester().getId() == user.getId()) {
                        return friend.getApprover();
                    }
                    else if (friend.getApprover().getId() == user.getId()) {
                        return friend.getRequester();
                    }
                    else return null;
                })
                .collect(Collectors.toCollection(ArrayList<User>::new));
        return friendsList;
    }

    //NUMBER OF FRIEND REQUESTS
    @RequestMapping(path = "/requestAmt", method = RequestMethod.GET)
    public int friendRequestsAmt (HttpSession session) {
        User user = users.findByUsername((String) session.getAttribute("username"));
        List<Friend> allList = (List<Friend>) friends.findAll();
        List<User> requestList = new ArrayList<>();
        for (Friend f : allList) {

            // populating requestList with users who "friended" current user
            if (f.getApprover().getId()==user.getId()) {
                requestList.add(f.getRequester());

                // removing users from requestList who have been "friended back" by current user
                for(Friend f2 : allList) {
                    if (f2.getRequester().getId() == user.getId()) {
                        requestList.remove(f2.getApprover());
                    }
                }
            }
        }
        // requestList.size == number of pending requests
        return requestList.size();
    }

    //LIST OF ACTUAL FRIEND REQUESTS
    @RequestMapping(path = "/requests", method = RequestMethod.GET)
    public List<User> friendRequests (HttpSession session) throws Exception {
        User user = users.findByUsername((String) session.getAttribute("username"));
        List<Friend> allList = (List<Friend>) friends.findAll();
        List<User> requestList = new ArrayList<>();
        if (allList != null) {
            for (Friend f : allList) {

                // populating requestList with users who "friended" current user
                if (f.getApprover().getId() == user.getId()) {
                    requestList.add(f.getRequester());

                    // removing users from requestList who have been "friended back" by current user
                    for (Friend ff : allList) {
                        if (ff.getRequester().getId() == user.getId()) {
                            requestList.remove(ff.getApprover());
                        }
                    }
                }
            }
        }
        if (requestList != null) {
            return requestList;
        }
        else {
            throw new Exception("No friend requests have been made for this user");
        }
    }

    //DISPLAY PROFILE
    @RequestMapping(path = "/user/{id}", method = RequestMethod.GET)
    public User showProfile (@PathVariable("id") int id) {
        User user = users.findOne(id);
        return user;
    }

    //DISPLAY USERS WHO JOINED A SESH (ID = SESH ID)
    @RequestMapping(path = "/sesh/{id}", method = RequestMethod.GET)
    public List<User> joinedUsers (@PathVariable("id") int id) {
        Sesh sesh = seshs.findOne(id);
        List<User> joined = new ArrayList<>();
        List<Join> all = (List<Join>) joins.findAll();
        for (Join j : all) {
            if (j.getSesh().getId() == id) {
                joined.add(j.getUser());
            }
        }
        joined.remove(sesh.getUser()); // removes the sesh creator from the list
        return joined;
    }

    //RETURNS A SINGLE SESH OBJECT (ID = SESH ID)
    @RequestMapping(path = "/sesh/{id}/coords", method = RequestMethod.GET)
    public Sesh oneSesh (@PathVariable("id") int id) {
        Sesh s = seshs.findOne(id);
        return s;
    }

    //EDIT EXISTING SESH
    @RequestMapping(path = "/sesh", method = RequestMethod.PUT)
    public void editSesh (@RequestBody Sesh sesh, HttpSession session) {
        User u = users.findByUsername((String) session.getAttribute("username"));
        if (u.getId() == sesh.getUser().getId()) {
            seshs.save(sesh);
        }
    }

    //DELETE A SESSION
    @RequestMapping(path = "/sesh/{id}", method = RequestMethod.DELETE)
    public void deleteSesh (@PathVariable("id") int id) {
        Sesh sesh = seshs.findOne(id);
        seshs.delete(sesh);
    }

    /**
     * This will remove two Friend objects: the original and its reciprocal.
     * @param id
     * @param session
     */
    //REMOVE SOMEONE FROM FRIENDS LIST
    @RequestMapping(path = "/friend/{id}", method = RequestMethod.DELETE)
    public void removeFriend (@PathVariable("id") int id, HttpSession session) {
        User loggedInUser = users.findByUsername((String) session.getAttribute("username"));
        User friendToRemove = users.findOne(id);
        Friend friend = friends.findFirstByRequesterAndApprover(loggedInUser, friendToRemove);
        Friend friend2 = friends.findFirstByRequesterAndApprover(friendToRemove, loggedInUser);
        friends.delete(friend);
        friends.delete(friend2);
    }

    //DENY FRIEND REQUEST (THE ID = FRIENDING USER ID)
    @RequestMapping(path = "/deny/{id}", method = RequestMethod.DELETE)
    public void denyFriendRequest (@PathVariable("id") int id, HttpSession session) {
        User loggedIn = users.findByUsername((String) session.getAttribute("username"));
        User requester = users.findOne(id);
        Friend friend = friends.findFirstByRequesterAndApprover(requester, loggedIn);
        friends.delete(friend);
    }
}
