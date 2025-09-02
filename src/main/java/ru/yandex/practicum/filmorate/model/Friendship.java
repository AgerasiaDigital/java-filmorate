package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class Friendship {
    private Integer userId;
    private Integer friendId;
    private Boolean confirmed;

    public Friendship() {}

    public Friendship(Integer userId, Integer friendId, Boolean confirmed) {
        this.userId = userId;
        this.friendId = friendId;
        this.confirmed = confirmed;
    }
}