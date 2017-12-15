package com.qunar.qunit.sample.model;

public class Group {
    private int id;

    private String name;

    public com.qunar.qunit.sample.model.User[] getUsers() {
        return users;
    }

    public void setUsers(com.qunar.qunit.sample.model.User[] users) {
        this.users = users;
    }

    private com.qunar.qunit.sample.model.User[] users;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
