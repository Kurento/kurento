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
#include <iostream>
#include "Container3d.h"
using namespace alvar;
using namespace std;

template <class T>
class Container3dSortX {
protected:
	Container3d<T> &container;
public:
	Container3dSortX(Container3d<T> &_container) : container(_container) {}
	bool operator()(size_t i1, size_t i2) const {
		return (container[i1].first.x < container[i2].first.x);
	}
};

template <class T>
class Container3dLimitX {
protected:
	int x_min, x_max;
	Container3d<T> &container;
public:
	Container3dLimitX(Container3d<T> &_container, int _x_min, int _x_max) 
	: container(_container),x_min(_x_min),x_max(_x_max) {}
	bool operator()(size_t i1) const {
		if ((container[i1].first.x >= x_min) && (container[i1].first.x <= x_max)) return true;
		return false;
	}
};

class TestContainer3d : public CxxTest::TestSuite 
{
protected:
	Container3d<int> c3d;
public:
	TestContainer3d() {
		c3d.Add(cvPoint3D32f(0,0,0), 0);
		c3d.Add(cvPoint3D32f(5,0,0), 1);
		c3d.Add(cvPoint3D32f(0,5,0), 2);
		c3d.Add(cvPoint3D32f(0,0,5), 3);
		c3d.Add(cvPoint3D32f(0,0,500), 4);
		c3d.Add(cvPoint3D32f(500,0,0), 5);
		c3d.Add(cvPoint3D32f(1,0,0), 6);
		c3d.Add(cvPoint3D32f(0,0,1), 7);
	}
	~TestContainer3d() {
	}
	void testSortDistance0() {
		cout<<"\n### Sort Distance 0,0,0 ###"<<endl;
		Container3dSortDist<int> sort_dist(c3d, cvPoint3D32f(0,0,0));
		c3d.Sort(sort_dist); // Right answer: 0,6,7,1,2,3,4,5
		Container3d<int>::Iterator iter;

		iter=c3d.begin();
		TS_ASSERT(iter->second == 0);
		TS_ASSERT(iter->first.x == 0);
		TS_ASSERT(iter->first.y == 0);
		TS_ASSERT(iter->first.z == 0);
		++iter; TS_ASSERT(iter->second == 6);
		++iter; TS_ASSERT(iter->second == 7);
		++iter; TS_ASSERT(iter->second == 1);
		++iter; TS_ASSERT(iter->second == 2);
		++iter; TS_ASSERT(iter->second == 3);
		++iter; TS_ASSERT(iter->second == 4);
		++iter; TS_ASSERT(iter->second == 5); 
		TS_ASSERT(iter->first.x == 500);
		TS_ASSERT(iter->first.y == 0);
		TS_ASSERT(iter->first.z == 0);
		++iter; TS_ASSERT(iter == c3d.end());
	}
	void testSortDistance1() {
		cout<<"\n### Sort Distance 490,0,0 ###"<<endl;
		Container3dSortDist<int> sort_dist(c3d, cvPoint3D32f(490,0,0));
		c3d.Sort(sort_dist); // Right answer: 5,1,6,0,7,2,3,4
		Container3d<int>::Iterator iter;

		iter=c3d.begin();
		TS_ASSERT(iter->second == 5);
		TS_ASSERT(iter->first.x == 500);
		TS_ASSERT(iter->first.y == 0);
		TS_ASSERT(iter->first.z == 0);
		++iter; TS_ASSERT(iter->second == 1);
		++iter; TS_ASSERT(iter->second == 6);
		++iter; TS_ASSERT(iter->second == 0);
		++iter; TS_ASSERT(iter->second == 7);
		++iter; TS_ASSERT(iter->second == 2);
		++iter; TS_ASSERT(iter->second == 3);
		++iter; TS_ASSERT(iter->second == 4); 
		TS_ASSERT(iter->first.x == 0);
		TS_ASSERT(iter->first.y == 0);
		TS_ASSERT(iter->first.z == 500);
		++iter; TS_ASSERT(iter == c3d.end());
	}
	void testReset() {
		cout<<"\n### Reset ###"<<endl;
		c3d.ResetSearchSpace(); // Right answer: 0,1,2,3,4,5,6,7
		Container3d<int>::Iterator iter;

		iter=c3d.begin();
		TS_ASSERT(iter->second == 0);
		TS_ASSERT(iter->first.x == 0);
		TS_ASSERT(iter->first.y == 0);
		TS_ASSERT(iter->first.z == 0);
		++iter; TS_ASSERT(iter->second == 1);
		++iter; TS_ASSERT(iter->second == 2);
		++iter; TS_ASSERT(iter->second == 3);
		++iter; TS_ASSERT(iter->second == 4);
		++iter; TS_ASSERT(iter->second == 5);
		++iter; TS_ASSERT(iter->second == 6);
		++iter; TS_ASSERT(iter->second == 7); 
		TS_ASSERT(iter->first.x == 0);
		TS_ASSERT(iter->first.y == 0);
		TS_ASSERT(iter->first.z == 1);
		++iter; TS_ASSERT(iter == c3d.end());
	}
	void testSortX() {
		cout<<"\n### Sort based on X coordinate ###"<<endl;
		Container3dSortX<int> sortx(c3d);
		c3d.Sort(sortx); // Right answer: 0,2,3,4,7,6,1,5
		Container3d<int>::Iterator iter;

		iter=c3d.begin();
		TS_ASSERT(iter->second == 0);
		TS_ASSERT(iter->first.x == 0);
		TS_ASSERT(iter->first.y == 0);
		TS_ASSERT(iter->first.z == 0);
		++iter; TS_ASSERT(iter->second == 2);
		++iter; TS_ASSERT(iter->second == 3);
		++iter; TS_ASSERT(iter->second == 4);
		++iter; TS_ASSERT(iter->second == 7);
		++iter; TS_ASSERT(iter->second == 6);
		++iter; TS_ASSERT(iter->second == 1);
		++iter; TS_ASSERT(iter->second == 5); 
		TS_ASSERT(iter->first.x == 500);
		TS_ASSERT(iter->first.y == 0);
		TS_ASSERT(iter->first.z == 0);
		++iter; TS_ASSERT(iter == c3d.end());
	}
	void testLimit() {
		cout<<"\n### Limit distance to 10 based on X coordinate ###"<<endl;
		c3d.ResetSearchSpace();
		Container3dLimitDist<int> limit_dist(c3d, cvPoint3D32f(0,0,0), 10.0);
		c3d.Limit(limit_dist); // Right answer: 0,1,2,3,6,7
		Container3d<int>::Iterator iter;

		iter=c3d.begin(); TS_ASSERT(iter->second == 0);
		++iter; TS_ASSERT(iter->second == 1);
		++iter; TS_ASSERT(iter->second == 2);
		++iter; TS_ASSERT(iter->second == 3);
		++iter; TS_ASSERT(iter->second == 6);
		++iter; TS_ASSERT(iter->second == 7);
		++iter; TS_ASSERT(iter == c3d.end());
	}
	void testSortXLimit() {
		cout<<"\n### Sort based on X coordinate and limit distance to 10 ###"<<endl;
		Container3dSortX<int> sortx(c3d);
		Container3dLimitDist<int> limit_dist(c3d, cvPoint3D32f(0,0,0), 10.0);
		c3d.ResetSearchSpace(); // Search space: 0,1,2,3,4,5,6,7
		c3d.Sort(sortx);        // Search space: 0,2,3,4,7,6,1,5
		c3d.Limit(limit_dist);  // Search space: 0,2,3,7,6,1
		Container3d<int>::Iterator iter;

		iter=c3d.begin(); TS_ASSERT(iter->second == 0);
		++iter; TS_ASSERT(iter->second == 2);
		++iter; TS_ASSERT(iter->second == 3);
		++iter; TS_ASSERT(iter->second == 7);
		++iter; TS_ASSERT(iter->second == 6);
		++iter; TS_ASSERT(iter->second == 1);
		++iter; TS_ASSERT(iter == c3d.end());
	}
	void testLimitX() {
		cout<<"\n### Limit based on X coordinate (-10 ... 10) ###"<<endl;
		Container3dLimitX<int> limitx(c3d, -10, 10);
		c3d.ResetSearchSpace(); // Search space: 0,1,2,3,4,5,6,7
		c3d.Limit(limitx);      // Search space: 0,1,2,3,4,6,7
		Container3d<int>::Iterator iter;

		iter=c3d.begin(); TS_ASSERT(iter->second == 0);
		++iter; TS_ASSERT(iter->second == 1);
		++iter; TS_ASSERT(iter->second == 2);
		++iter; TS_ASSERT(iter->second == 3);
		++iter; TS_ASSERT(iter->second == 4);
		++iter; TS_ASSERT(iter->second == 6);
		++iter; TS_ASSERT(iter->second == 7);
		++iter; TS_ASSERT(iter == c3d.end());

		/*for (iter=c3d.begin(); iter != c3d.end(); ++iter) {
			cout<<"P: "<<iter->second<<endl;
		}*/
	}
};
