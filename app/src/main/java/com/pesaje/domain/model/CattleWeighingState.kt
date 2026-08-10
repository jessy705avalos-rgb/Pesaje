package com.pesaje.domain.model

enum class CattleWeighingState {
    WAITING_FOR_ANIMAL, //esperando q suba un animal
    STABILIZING,        //animal sobre la bascula
    LOCKED,             //peso estable captura y congelado
    WAITING_FOR_EXIT    //esperando a que el animal baje para resetear
}