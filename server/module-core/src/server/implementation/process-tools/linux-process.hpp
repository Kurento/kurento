/*
 * (C) Copyright 2019 Kurento (https://www.kurento.org/)
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

#ifndef _KMS_PROCESS_TOOLS_H_
#define _KMS_PROCESS_TOOLS_H_

/**
 * Total number of CPUs.
 *
 * Note from source file "coreutils/lib/nproc.h":
 * A "processor" in this context means a thread execution unit, that is either:
 *  - an execution core in a (possibly multi-core) chip, in a (possibly
 *    multi-chip) module, in a single computer, or
 *  - a thread execution unit inside a core (hyper-threading, see
 *    <http://en.wikipedia.org/wiki/Hyper-threading>).
 * Which of the two definitions is used, is unspecified.
 */
unsigned long cpuCount ();


struct cpustat_t {
  unsigned long processTicks;
  unsigned long systemTicks;
};

/**
 * Get CPU timings that are needed to calculate the CPU usage %.
 */
void cpuPercentBegin (struct cpustat_t *cpustat);

/**
 * Generate CPU usage % from the provided CPU timings.
 */
float cpuPercentEnd (const struct cpustat_t *cpustat);


/**
 * Memory used by this process, in KiB.
 * This counts the Resident Set Size (RSS).
 */
long int memoryUse ();

#endif /* _KMS_PROCESS_TOOLS_H_ */
