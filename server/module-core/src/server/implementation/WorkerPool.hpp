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

#ifndef __WORKERPOOL_HPP__
#define __WORKERPOOL_HPP__

#include <boost/asio/async_result.hpp>
#include <boost/asio/io_service.hpp>
#include <boost/asio/steady_timer.hpp>
#include <boost/thread/thread.hpp>

#include <chrono>

namespace kurento
{

class WorkerPool
{
public:
  /*
   * With `threads_count == 0`, it will automatically adapt to the number of
   * CPU cores that are available in the current environment.
   */
  WorkerPool (size_t threads_count = 0);
  ~WorkerPool ();

  template <typename CompletionHandler>
  BOOST_ASIO_INITFN_RESULT_TYPE (CompletionHandler, void ())
  post (BOOST_ASIO_MOVE_ARG (CompletionHandler) handler)
  {
    // `io_service::post()` assigns new tasks to the thread pool
    return io_service.post (handler);
  }

private:
  // Boost Asio tools for handling a thread pool
  boost::asio::io_service io_service; // Boost Asio task runner
  boost::thread_group io_threadpool;

  /*
   * Keeping an instance of `io_service::work` tells `io_service` to keep
   * running its internal loop, even after all tasks have been serviced.
   *
   * Think of it as a loop condition check:
   *
   *     while (!stopped && "work instance exists") {
   *         process_tasks()
   *     }
   */
  boost::asio::io_service::work io_work;

  // Thread pool health check
  void checkThreads ();
  boost::asio::steady_timer check_timer;
  std::chrono::steady_clock::time_point check_time_last;

  class StaticConstructor
  {
  public:
    StaticConstructor ();
  };

  static StaticConstructor staticConstructor;
};

} // namespace kurento

#endif /* __WORKERPOOL_HPP__ */
