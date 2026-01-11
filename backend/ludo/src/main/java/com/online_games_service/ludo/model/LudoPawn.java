package com.online_games_service.ludo.model;

import java.io.Serializable;

import com.online_games_service.ludo.enums.PlayerColor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LudoPawn implements Serializable{
    private int id;         
    private int position;   
    private PlayerColor color;
    private int stepsMoved;
    private boolean inBase; 
    private boolean inHome; 
}