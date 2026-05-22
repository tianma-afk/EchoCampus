package com.echocampus.service.user.impl;


import com.echocampus.mapper.LandmarkMapper;

import com.echocampus.service.user.LandmarkService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LandmarkServiceImpl implements LandmarkService {
	private final LandmarkMapper landmarkMapper;

	public LandmarkServiceImpl(LandmarkMapper landmarkMapper) {
		this.landmarkMapper = landmarkMapper;
	}

	@Override
	public void getLandmarkDetail() {
	}
}
