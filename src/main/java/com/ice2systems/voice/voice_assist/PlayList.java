package com.ice2systems.voice.voice_assist;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class PlayList {
	List<InputStream> list = new LinkedList<InputStream>();
	long shift;
	long relativeShift;
  long id;
	
	public PlayList(long id,long shift) {
		this.id = id;
		this.shift = shift;
	}
	
	public void add2List(final InputStream stream) {
		list.add(stream);
	}
}
