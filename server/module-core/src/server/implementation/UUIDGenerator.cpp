/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

#include <boost/uuid/uuid.hpp>
#include <boost/uuid/uuid_generators.hpp>
#include <boost/uuid/uuid_io.hpp>
#include <chrono>
#include <sstream>
#include <sys/types.h>
#include <unistd.h>

namespace kurento
{

class RandomGenerator
{
  boost::uuids::basic_random_generator<boost::mt19937> gen;
  boost::mt19937 ran;
  pid_t pid{};

public:
  RandomGenerator () : gen (&ran)
  {
    init ();
  }

  void init ()
  {
    std::chrono::high_resolution_clock::time_point now =
      std::chrono::high_resolution_clock::now();

    std::chrono::nanoseconds time =
      std::chrono::duration_cast<std::chrono::nanoseconds>
      (now.time_since_epoch () );

    ran.seed (time.count() );

    pid = getpid();
  }

  void reinit ()
  {
    if (pid != getpid() ) {
      init();
    }
  }

  std::string getUUID ()
  {
    reinit();

    std::stringstream ss;
    boost::uuids::uuid uuid = gen ();

    ss << uuid;
    return ss.str();
  }
};

static RandomGenerator gen;

std::string
generateUUID ()
{
  return gen.getUUID ();
}

}
