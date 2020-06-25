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
#include <math.h>
#include <stdio.h>
#include "Ransac.h"

using namespace alvar;

class TestRansac : public CxxTest::TestSuite {

  typedef float MyModel;

  typedef float MyParameter;

  class MyEstimator : public Ransac<MyModel, MyParameter> {
  public:
    MyEstimator() : Ransac<MyModel, MyParameter>(1, 10) {} // min params, max params

  protected:
    void doEstimate(MyParameter** params, int param_c,
                    MyModel* model) {
      float avg = 0;
      for (int i = 0; i < param_c; i++) {
        avg += *params[i];
      }
      *model = avg / param_c;
    }

    bool doSupports(MyParameter* param, MyModel* model) {
      return fabs(*model - *param) < 1.5;
    }
  };

public:
  void testRansac() {
    printf("\n");

#define param_c 10
    MyParameter params[param_c] = {
      0, -1, 1, 0.5, -0.5, 3, 4, 5, -3, 0.25
    };

    MyEstimator estimator;
    MyModel model = -100;
    int support = estimator.estimate(params, param_c, 
                                     5, 10, // support limit, max rounds
                                     &model);

    printf("Estimated model: %f, supporting parameters: %d\n", 
           model, support);

    TS_ASSERT_EQUALS( 5, support );
    TS_ASSERT_DELTA( 0, model, 0.5 );

    support = estimator.refine(params, param_c,
                               6, 10,
                               &model); 

    printf("Refined model: %f, supporting parameters: %d\n", 
           model, support);

    TS_ASSERT_EQUALS( 6, support );
    TS_ASSERT_DELTA( 0.042, model, 0.001 );
  }

};
