package com.kurento.kmf.media;

import com.kurento.tool.rom.server.Param;

public class WindowParam {

    private int topRightCornerX;
    private int topRightCornerY;
    private int width;
    private int height;

    public WindowParam(@Param("topRightCornerX") int topRightCornerX, @Param("topRightCornerY") int topRightCornerY, @Param("width") int width, @Param("height") int height){
        this.topRightCornerX = topRightCornerX;
        this.topRightCornerY = topRightCornerY;
        this.width = width;
        this.height = height;
    }

    public int getTopRightCornerX(){
    	return topRightCornerX;
    }

    public void setTopRightCornerX(int topRightCornerX){
    	this.topRightCornerX = topRightCornerX;
    }

    public int getTopRightCornerY(){
    	return topRightCornerY;
    }

    public void setTopRightCornerY(int topRightCornerY){
    	this.topRightCornerY = topRightCornerY;
    }

    public int getWidth(){
    	return width;
    }

    public void setWidth(int width){
    	this.width = width;
    }

    public int getHeight(){
    	return height;
    }

    public void setHeight(int height){
    	this.height = height;
    }

}

