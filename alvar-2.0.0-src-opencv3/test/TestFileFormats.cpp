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
#include <string>
#include <vector>

#include "FileFormatUtils.h"
#include "MultiMarker.h"
#include "Camera.h"

using namespace alvar;
using namespace std;

class TestFileFormats : public CxxTest::TestSuite {

private:
  std::string path;

public:
  TestFileFormats() {
    path = std::string(ALVAR_TEST_DIR);
  }

  void testUtils_allocateXMLMatrix() {
    printf("\ntestUtils_allocateXMLMatrix\n");

		TiXmlDocument document;
		if (!document.LoadFile(path + "/test_matrix.xml")) return;
		TiXmlElement *xml_root = document.RootElement();

		CvMat* mat_1 = FileFormatUtils::allocateXMLMatrix(xml_root->FirstChildElement("matrix_32f"));
		CvMat* mat_2 = FileFormatUtils::allocateXMLMatrix(xml_root->FirstChildElement("matrix_64f"));
		CvMat* mat_3 = FileFormatUtils::allocateXMLMatrix(xml_root->FirstChildElement("matrix_error1"));
		CvMat* mat_4 = FileFormatUtils::allocateXMLMatrix(xml_root->FirstChildElement("matrix_error2"));
		CvMat* mat_5 = FileFormatUtils::allocateXMLMatrix(xml_root->FirstChildElement("matrix_error3"));

		TS_ASSERT( mat_1 != NULL );
		TS_ASSERT( mat_2 != NULL );
		TS_ASSERT( mat_3 == NULL );
		TS_ASSERT( mat_4 == NULL );
		TS_ASSERT( mat_5 == NULL );

		TS_ASSERT_EQUALS( cvGetElemType(mat_1), CV_32F );
		TS_ASSERT_EQUALS( mat_1->rows, 3 );
		TS_ASSERT_EQUALS( mat_1->cols, 4 );

		TS_ASSERT_EQUALS( cvGetElemType(mat_2), CV_64F );
		TS_ASSERT_EQUALS( mat_2->rows, 4 );
		TS_ASSERT_EQUALS( mat_2->cols, 3 );
		printf("end\n");
  }

  void testUtils_parseXMLMatrix() {
    printf("\ntestUtils_parseXMLMatrix\n");

		TiXmlDocument document;
		if (!document.LoadFile(path + "/test_matrix.xml")) return;
		TiXmlElement *xml_root = document.RootElement();

		CvMat* mat_1 = cvCreateMat(3, 4, CV_32F);
		CvMat* mat_2 = cvCreateMat(4, 3, CV_64F);
		CvMat* mat_3 = cvCreateMat(3, 3, CV_64F);
		CvMat* mat_4 = cvCreateMat(4, 4, CV_64F);

		TS_ASSERT( FileFormatUtils::parseXMLMatrix(xml_root->FirstChildElement("matrix_32f"), mat_1) );
		TS_ASSERT_EQUALS( cvGetElemType(mat_1), CV_32F );
		TS_ASSERT_EQUALS( mat_1->rows, 3 );
		TS_ASSERT_EQUALS( mat_1->cols, 4 );
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 0, 0), 1.1, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 0, 1), 1.2, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 0, 2), 1.3, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 0, 3), 1.4, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 1, 0), 2.1, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 1, 1), 2.2, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 1, 2), 2.3, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 1, 3), 2.4, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 2, 0), 3.1, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 2, 1), 3.2, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 2, 2), 3.3, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_1, 2, 3), 3.4, 0.00001 ); 

		TS_ASSERT( FileFormatUtils::parseXMLMatrix(xml_root->FirstChildElement("matrix_64f"), mat_2) );
		TS_ASSERT_EQUALS( cvGetElemType(mat_2), CV_64F );
		TS_ASSERT_EQUALS( mat_2->rows, 4 );
		TS_ASSERT_EQUALS( mat_2->cols, 3 );
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 0, 0), 1.1, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 0, 1), 1.2, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 0, 2), 1.3, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 1, 0), 2.1, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 1, 1), 2.2, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 1, 2), 2.3, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 2, 0), 3.1, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 2, 1), 3.2, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 2, 2), 3.3, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 3, 0), 4.1, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 3, 1), 4.2, 0.00001 ); 
		TS_ASSERT_DELTA( cvGetReal2D(mat_2, 3, 2), 0.00002314987189274, 0.000000001 ); 

		TS_ASSERT( !FileFormatUtils::parseXMLMatrix(xml_root->FirstChildElement("matrix_32f"), mat_2) );
		TS_ASSERT( !FileFormatUtils::parseXMLMatrix(xml_root->FirstChildElement("matrix_64f"), mat_1) );
		TS_ASSERT( !FileFormatUtils::parseXMLMatrix(xml_root->FirstChildElement("matrix_64f"), mat_3) );
		TS_ASSERT( !FileFormatUtils::parseXMLMatrix(xml_root->FirstChildElement("matrix_64f"), mat_4) );
		TS_ASSERT( !FileFormatUtils::parseXMLMatrix(xml_root->FirstChildElement("matrix_error1"), mat_2) );
		TS_ASSERT( !FileFormatUtils::parseXMLMatrix(xml_root->FirstChildElement("matrix_error2"), mat_2) );
		TS_ASSERT( !FileFormatUtils::parseXMLMatrix(xml_root->FirstChildElement("matrix_error3"), mat_2) );
		TS_ASSERT( !FileFormatUtils::parseXMLMatrix(xml_root->FirstChildElement("matrix_error4"), mat_2) );

		printf("end\n");
  }

  void testUtils_createXMLMatrix() {
    printf("\ntestUtils_createXMLMatrix\n");

		CvMat* mat_1 = cvCreateMat(3, 4, CV_32F);
		cvSetReal2D(mat_1, 0, 0, 1.1);
		cvSetReal2D(mat_1, 0, 1, 1.2);
		cvSetReal2D(mat_1, 0, 2, 1.3);
		cvSetReal2D(mat_1, 0, 3, 1.4);
		cvSetReal2D(mat_1, 1, 0, 2.1);
		cvSetReal2D(mat_1, 1, 1, 2.2);
		cvSetReal2D(mat_1, 1, 2, 2.3);
		cvSetReal2D(mat_1, 1, 3, 2.4);
		cvSetReal2D(mat_1, 2, 0, 3.1);
		cvSetReal2D(mat_1, 2, 1, 3.2);
		cvSetReal2D(mat_1, 2, 2, 3.3);
		cvSetReal2D(mat_1, 2, 3, 3.4);
		TiXmlElement* xml_1 = FileFormatUtils::createXMLMatrix("matrix_1", mat_1);
		TS_ASSERT( xml_1 );
		{
			TiXmlPrinter printer;
			printer.SetStreamPrinting();
			xml_1->Accept(&printer);
            const char str[] = "<matrix_1 type=\"CV_32F\" rows=\"3\" cols=\"4\"><data>1.1</data><data>1.2</data><data>1.3</data><data>1.4</data><data>2.0999999</data><data>2.2</data><data>2.3</data><data>2.4000001</data><data>3.0999999</data><data>3.2</data><data>3.3</data><data>3.4000001</data></matrix_1>";
			TS_ASSERT_EQUALS( string(printer.CStr()), string(str) );
		}

		CvMat* mat_2 = cvCreateMat(4, 3, CV_64F);
		cvSetReal2D(mat_2, 0, 0, 0.00002314987189274);
		cvSetReal2D(mat_2, 0, 1, 494.19838264911749);
		cvSetReal2D(mat_2, 0, 2, 1.3);
		cvSetReal2D(mat_2, 1, 0, 2.1);
		cvSetReal2D(mat_2, 1, 1, 2.2);
		cvSetReal2D(mat_2, 1, 2, 2.3);
		cvSetReal2D(mat_2, 2, 0, 3.1);
		cvSetReal2D(mat_2, 2, 1, 3.2);
		cvSetReal2D(mat_2, 2, 2, 3.3);
		cvSetReal2D(mat_2, 3, 0, 4.1);
		cvSetReal2D(mat_2, 3, 1, 4.2);
		cvSetReal2D(mat_2, 3, 2, 4.3);
		TiXmlElement* xml_2 = FileFormatUtils::createXMLMatrix("matrix_2", mat_2);
		TS_ASSERT( xml_2 );
		{
			TiXmlPrinter printer;
			printer.SetStreamPrinting();
			xml_2->Accept(&printer);
            #ifdef WIN32
                const char str[] = "<matrix_2 type=\"CV_64F\" rows=\"4\" cols=\"3\"><data>2.3149871892740001e-005</data><data>494.19838264911749</data><data>1.3</data><data>2.1000000000000001</data><data>2.2000000000000002</data><data>2.2999999999999998</data><data>3.1000000000000001</data><data>3.2000000000000002</data><data>3.2999999999999998</data><data>4.0999999999999996</data><data>4.2000000000000002</data><data>4.2999999999999998</data></matrix_2>";
            #else
                const char str[] = "<matrix_2 type=\"CV_64F\" rows=\"4\" cols=\"3\"><data>2.3149871892740001e-05</data><data>494.19838264911749</data><data>1.3</data><data>2.1000000000000001</data><data>2.2000000000000002</data><data>2.2999999999999998</data><data>3.1000000000000001</data><data>3.2000000000000002</data><data>3.2999999999999998</data><data>4.0999999999999996</data><data>4.2000000000000002</data><data>4.2999999999999998</data></matrix_2>";
            #endif
			TS_ASSERT_EQUALS( string(printer.CStr()), string(str) );
		}

		printf("end\n");
	}

  void testCameraLoadStore() {
    printf("\ntestCameraLoadStore\n");

		Camera cam1, cam2;

        TS_ASSERT( cam1.SetCalib((path + "/test_calib.xml").data(), 640, 480) );
		TS_ASSERT( cam1.SaveCalib("test_SaveCalib.xml", FILE_FORMAT_XML) );
		TS_ASSERT( cam2.SetCalib("test_SaveCalib.xml", 640, 480, FILE_FORMAT_XML) );

		TS_ASSERT_DELTA( cam1.calib_K_data[0][0], cam2.calib_K_data[0][0], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_K_data[0][1], cam2.calib_K_data[0][1], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_K_data[0][2], cam2.calib_K_data[0][2], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_K_data[1][0], cam2.calib_K_data[1][0], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_K_data[1][1], cam2.calib_K_data[1][1], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_K_data[1][2], cam2.calib_K_data[1][2], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_K_data[2][0], cam2.calib_K_data[2][0], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_K_data[2][1], cam2.calib_K_data[2][1], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_K_data[2][2], cam2.calib_K_data[2][2], 0.00001 );

		TS_ASSERT_DELTA( cam1.calib_D_data[0], cam2.calib_D_data[0], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_D_data[1], cam2.calib_D_data[1], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_D_data[2], cam2.calib_D_data[2], 0.00001 );
		TS_ASSERT_DELTA( cam1.calib_D_data[3], cam2.calib_D_data[3], 0.00001 );
		
		printf("end\n");
  }

  void testMultimarkerLoadStore() {
    printf("\ntestMultimarkerLoadStore\n");

		int nof_markers = 8;
		vector<int> id_vector;
		for(int i = 0; i < nof_markers; ++i)
			id_vector.push_back(i);

		MultiMarker multi_marker(id_vector);
		double marker_size = 10;
		Pose pose;
		multi_marker.PointCloudAdd(0, marker_size*2, pose);
		pose.SetTranslation(-marker_size*2.5, +marker_size*1.5, 0);
		multi_marker.PointCloudAdd(1, marker_size, pose);
		pose.SetTranslation(+marker_size*2.5, +marker_size*1.5, 0);
		multi_marker.PointCloudAdd(2, marker_size, pose);
		pose.SetTranslation(-marker_size*2.5, -marker_size*1.5, 0);
		multi_marker.PointCloudAdd(3, marker_size, pose);
		pose.SetTranslation(+marker_size*2.5, -marker_size*1.5, 0);
		multi_marker.PointCloudAdd(4, marker_size, pose);
		TS_ASSERT( multi_marker.Save("test_SaveMultimarker.xml", FILE_FORMAT_XML) );

		vector<int> empty_vector;
		MultiMarker multi_marker2(empty_vector);
		TS_ASSERT( multi_marker2.Load("test_SaveMultimarker.xml", FILE_FORMAT_XML) );
		
		for (int i = 0; i < nof_markers; ++i) {
			TS_ASSERT_EQUALS( multi_marker.IsValidMarker(i), multi_marker2.IsValidMarker(i) );

			if (multi_marker.IsValidMarker(i)) {
				for (int j = 0; j < 4; ++j) {
					double x1, y1, z1, x2, y2, z2;
					multi_marker.PointCloudGet(i, j, x1, y1, z1);
					multi_marker2.PointCloudGet(i, j, x2, y2, z2);
					TS_ASSERT_EQUALS( x1, x2 );
					TS_ASSERT_EQUALS( y1, y2 );
					TS_ASSERT_EQUALS( z1, z2 );
				}
			}
		}

		printf("end\n");
  }
};
