/*
 * This file is part of ALVAR, A Library for Virtual and Augmented Reality.
 *
 * Copyright 2007-2012 VTT Technical Research Centre of Finland
 *
 * Contact: VTT Augmented Reality Team <alvar.info@vtt.fi>
 *          <http://www.vtt.fi/multimedia/alvar.html>
 *
 * ALVAR is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ALVAR; if not, see
 * <http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html>.
 */

#include <cxxtest/TestSuite.h>
#include <stdio.h>
#include "Camera.h"
#include "Pose.h"

#undef _U
#undef _P
#include "TrifocalTensor.h"
using namespace alvar;
using namespace std;

class TestTrifocalTensor : public CxxTest::TestSuite {

public:
  void testSimpleProjection2() {
    printf("\n");
    const double p3d_z = 10.;
    const int p3d_c = 8;
    CvPoint3D64f p3d[p3d_c] = {
      { 1, 1, p3d_z+1 },
      { 1,-1, p3d_z+1 },
      {-1,-1, p3d_z+1 },
      {-1, 1, p3d_z+1 },
      { 1, 1, p3d_z-1 },
      { 1,-1, p3d_z-1 },
      {-1,-1, p3d_z-1 },
      {-1, 1, p3d_z-1 }
    };

    Camera camera;
    Pose P0, P1, P2;
    P1.SetTranslation(1, 0, 0);
    P2.SetTranslation(2, 0, 0);
    TrifocalTensor tt(P1, P2);

    for (int i = 0; i < p3d_c; i++) {
      CvPoint2D64f p2d_0, p2d_1, p2d_2, p2d_est;
      vector<CvPoint2D64f> vec2d;
      vector<CvPoint3D64f> vec3d;
      vec2d.resize(1);
      vec3d.push_back(p3d[i]);
      camera.ProjectPoints(vec3d, &P0, vec2d); p2d_0 = vec2d[0];
      camera.ProjectPoints(vec3d, &P1, vec2d); p2d_1 = vec2d[0];
      camera.ProjectPoints(vec3d, &P2, vec2d); p2d_2 = vec2d[0];

      tt.project(p2d_0, p2d_1, p2d_est);

      printf("Real: %+5.2f, %+5.2f Est.: %+5.2f, %+5.2f\n",
	     p2d_2.x, p2d_2.y, p2d_est.x, p2d_est.y);

      double error = tt.projectError(p2d_0, p2d_1, p2d_2);
      TS_ASSERT_DELTA( 0, error, 0.1);
    }
  }

  void testSimpleProjection() {
    printf("\n");
    Camera camera;
    Pose p1, p2, p3;
    p2.SetTranslation(2, 0, 0);
    p3.SetTranslation(4, 0, 0);

    CvPoint3D64f p3d = {0, 0, 10};
    CvPoint2D64f p2d_1, p2d_2, p2d_3;
    vector<CvPoint2D64f> vec2d;
    vector<CvPoint3D64f> vec3d;
    vec2d.resize(1);
    vec3d.push_back(p3d);
    camera.ProjectPoints(vec3d, &p1, vec2d); p2d_1 = vec2d[0];
    camera.ProjectPoints(vec3d, &p2, vec2d); p2d_2 = vec2d[0];
    camera.ProjectPoints(vec3d, &p3, vec2d); p2d_3 = vec2d[0];

    printf("Point 1: %f, %f\n", p2d_1.x, p2d_1.y);
    printf("Point 2: %f, %f\n", p2d_2.x, p2d_2.y);
    printf("Point 3: %f, %f\n", p2d_3.x, p2d_3.y);

    TrifocalTensor tt(p2, p3);
    CvPoint2D64f pp;
    tt.project(p2d_1, p2d_2, pp);
    printf("Point 3: %f, %f\n", pp.x, pp.y);

    double error = tt.projectError(p2d_1, p2d_2, p2d_3);
    TS_ASSERT_DELTA( 0, error, 0.1);
  }
};
