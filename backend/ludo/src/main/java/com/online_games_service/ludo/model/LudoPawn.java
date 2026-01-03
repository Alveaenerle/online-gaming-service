package com.online_games_service.ludo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LudoPawn {
    private int id;         
    private int position;   
    private boolean inBase; 
    private boolean inHome; 
}