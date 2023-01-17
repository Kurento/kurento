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
#include "UnscentedKalman.h"
#include <stdio.h>
using namespace alvar;

static double dt = 0.01;
static double xr = 6374.;
static double yr = 0.;

#define getPos(state, x, y) \
{ \
  x = cvGetReal1D(state, 0); \
  y = cvGetReal1D(state, 1); \
}

#define getVel(state, x, y) \
{ \
  x = cvGetReal1D(state, 2); \
  y = cvGetReal1D(state, 3); \
}

#define getCoeff(state) cvGetReal1D(state, 4) 

#define setPos(state, pos_x, pos_y) \
{ \
  cvSetReal1D(state, 0, pos_x); \
  cvSetReal1D(state, 1, pos_y); \
}

#define setVel(state, vel_x, vel_y) \
{ \
  cvSetReal1D(state, 2, vel_x); \
  cvSetReal1D(state, 3, vel_y); \
}

#define setCoeff(state, coeff) cvSetReal1D(state, 4, coeff)

#define setObservation(obs, r, t) \
{ \
  cvSetReal1D(obs, 0, r); \
  cvSetReal1D(obs, 1, t); \
}


class SimpleFilter 
  : public UnscentedKalman, public UnscentedProcess {
public:
  CvMat *processNoise;

  SimpleFilter(int k = 1) : UnscentedKalman(5, 2, k) {

    cvSetReal2D(getStateCovariance(), 0, 0, 1e-6);
    cvSetReal2D(getStateCovariance(), 1, 1, 1e-6);
    cvSetReal2D(getStateCovariance(), 2, 2, 1e-6);
    cvSetReal2D(getStateCovariance(), 3, 3, 1e-6);
    cvSetReal2D(getStateCovariance(), 4, 4, 1);

    processNoise = cvCreateMat(5, 5, CV_64F);
    cvSetReal2D(processNoise, 2, 2, 2.4064e-5);
    cvSetReal2D(processNoise, 3, 3, 2.4064e-5);
  }

  ~SimpleFilter() {
    cvReleaseMat(&processNoise);
  }

  CvMat *getProcessNoise() {
    return processNoise;
  }

  void f(CvMat *state) {
    // replace x with estimate of the next state.

    double pos_x, pos_y, vel_x, vel_y, coeff;
    getPos(state, pos_x, pos_y);
    getVel(state, vel_x, vel_y);
    coeff = getCoeff(state);

    double b = 0.59783*exp(coeff);
    double R = sqrt(pos_x*pos_x + pos_y*pos_y);
    double V = sqrt(vel_x*vel_x + vel_y*vel_y);
    double D = -b*exp((6374.-R)/13.406)*V;
    double G = -3.9860e5 / (R*R*R);
    
    double dvx = (D*vel_x + G*pos_x /*+ q1*/);
    double dvy = (D*vel_y + G*pos_y /*+ q2*/);

    setPos(state, pos_x + vel_x*dt, pos_y + vel_y*dt);
    setVel(state, vel_x + dvx*dt, vel_y + dvy*dt);
    setCoeff(state, coeff /*+ q3*dt*/);
  }
};

class SimpleObservation : public UnscentedObservation {
public:
  CvMat *z;
  CvMat *noise;

  SimpleObservation() {
    z = cvCreateMat(2, 1, CV_64F);
    noise = cvCreateMat(2, 2, CV_64F);

    cvSetReal2D(noise, 0, 0, 1e-3);
    cvSetReal2D(noise, 1, 1, .17e-3);
  }

  ~SimpleObservation() {
    cvReleaseMat(&z);
    cvReleaseMat(&noise);
  }

  void h(CvMat *z, CvMat *state) {
    // compute observation estimate z from current state estimate x.
    double pos_x, pos_y;
    getPos(state, pos_x, pos_y);

    double r = sqrt(pow(pos_x-xr,2)+pow(pos_y-yr,2));
    double t = atan((pos_y-yr)/(pos_x-xr));

    setObservation(z, r, t);
  }

  CvMat *getObservation() {
    return z;
  }

  CvMat *getObservationNoise() {
    return noise;
  }
};

class TestUnscentedKalman : public CxxTest::TestSuite {
public:
  void testReentryVecicleTracking() {
    printf("\n");
    srand(12345);

    SimpleFilter filter(1);
    SimpleObservation observation;
          
    setPos(filter.getState(), 6500.4, 349.14);
    setVel(filter.getState(), -1.8093, -6.7967);
    setCoeff(filter.getState(), 0);

    double real_pos_x = 6500.4;
    double real_pos_y = 349.14;
    double real_vel_x = -1.8093;
    double real_vel_y = -6.7967;
    double real_coeff = 0.6932;

    cv::RNG r_noise;
    cv::RNG t_noise;
    cv::RNG v_noise;

    for (int i = 0; i < 100000 && real_pos_x > 6374.; i++) {
      // simulate movement.
      double b = 0.59783*exp(real_coeff);
      double R = sqrt(real_pos_x*real_pos_x + real_pos_y*real_pos_y);
      double V = sqrt(real_vel_x*real_vel_x + real_vel_y*real_vel_y);
      double D = -b*exp((6374.-R)/13.406)*V;
      double G = -3.9860e5 / (R*R*R);

      double dvx = (D*real_vel_x + G*real_pos_x + v_noise.gaussian(2.4064e-5));
      double dvy = (D*real_vel_y + G*real_pos_y + v_noise.gaussian(2.4064e-5));

      real_pos_x += real_vel_x*dt /*+ 0.5*dvx*dt*dt*/;
      real_pos_y += real_vel_y*dt /*+ 0.5*dvy*dt*dt*/;
      real_vel_x += dvx*dt;
      real_vel_y += dvy*dt;

      // measure current position.
      SimpleObservation input;
      double r = sqrt(pow(real_pos_x-xr,2)+pow(real_pos_y-yr,2)) 
        + r_noise.gaussian(1e-3);
      double t = atan((real_pos_y-yr)/(real_pos_x-xr)) 
        + t_noise.gaussian(.17e-3);
      setObservation(input.z, r, t);
  	  
      // update filter
      filter.predict(&filter);
      filter.update(&input);

      static double time = 0;
      time += dt;
      if (i % 100 == 0) {
        printf("T: %6.3f", time);
        // print real and estimated values.
        /*
	  printf("Real Pos: %7.2f %6.2f Vel: %7.4f %7.4f Coeff: %6.4f",
	  real_pos_x, real_pos_y, real_vel_x, real_vel_y, real_coeff);
        */
        printf("  Real X: %7.2f dX: %7.4f b: %6.4f",
	       real_pos_x, real_vel_x, real_coeff);
        double pos_x, pos_y, vel_x, vel_y, coeff;
        getPos(filter.getState(), pos_x, pos_y);
        getVel(filter.getState(), vel_x, vel_y);
        coeff = getCoeff(filter.getState());
        /*
	  printf("  Est. Pos: %7.2f %6.2f Vel: %7.4f %7.4f Coeff: %6.4f\n",
	  pos_x, pos_y, vel_x, vel_y, coeff);
        */
        printf(" | Est. X: %7.2f dX: %7.4f b: %6.4f",
	       pos_x, vel_x, coeff);
        /*
	  printf("  Err Pos: %7.4f %7.4f Vel: %7.4f %7.4f Coeff: %6.4f\n",
	  pow(real_pos_x-pos_x,2),
	  pow(real_pos_y-pos_y,2),
	  pow(real_vel_x-vel_x,2),
	  pow(real_vel_y-vel_y,2),
	  pow(real_coeff-coeff,2));
        */
        printf(" | Err X: %.7f dX: %.7f b: %.7f\n",
	       fabs(real_pos_x-pos_x),
	       fabs(real_vel_x-vel_x),
	       fabs(real_coeff-coeff));
      }

    }
  }
};
