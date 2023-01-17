#!/bin/bash

function benchmark {
    echo ----------
    echo

    /usr/bin/time -o time.out $1 $2 $3
    echo

    cat time.out
    echo

    TIME=`cat time.out | grep user | awk '{print $1}' | sed s/user//`
    RESULT=`calc "print $4 * $TIME / 1000"`
    echo "$4 instructions/second  *  $TIME seconds  /  1000 detections  =  $RESULT instructions/detection"
    RESULT=`calc "print $TIME / 1000"`
    echo "$TIME seconds  /  1000 detections  =  $RESULT seconds/detection"
    echo
}

if [ $1 ]; then
    BENCHMARK=`pwd`/$1
else
    BENCHMARK=`pwd`/test/BenchmarkOverall
fi

PWD=`pwd`
cd `dirname $0`

FREQ=`cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq`
FREQ_MAX=`cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies | awk '{print $1}'`

echo $FREQ_MAX > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq

cat /proc/cpuinfo
BOGOMIPS=`cat /proc/cpuinfo | grep bogomips | awk '{print $3}'`

benchmark $BENCHMARK 1 ""   $BOGOMIPS
benchmark $BENCHMARK 1 "-t" $BOGOMIPS
benchmark $BENCHMARK 2 ""   $BOGOMIPS
benchmark $BENCHMARK 2 "-t" $BOGOMIPS
benchmark $BENCHMARK 3 ""   $BOGOMIPS
benchmark $BENCHMARK 3 "-t" $BOGOMIPS
benchmark $BENCHMARK 4 ""   $BOGOMIPS
benchmark $BENCHMARK 4 "-t" $BOGOMIPS
benchmark $BENCHMARK 5 ""   $BOGOMIPS
benchmark $BENCHMARK 5 "-t" $BOGOMIPS
benchmark $BENCHMARK 6 ""   $BOGOMIPS
benchmark $BENCHMARK 6 "-t" $BOGOMIPS
benchmark $BENCHMARK 7 ""   $BOGOMIPS
benchmark $BENCHMARK 7 "-t" $BOGOMIPS
benchmark $BENCHMARK 8 ""   $BOGOMIPS
benchmark $BENCHMARK 8 "-t" $BOGOMIPS
benchmark $BENCHMARK 9 ""   $BOGOMIPS
benchmark $BENCHMARK 9 "-t" $BOGOMIPS
benchmark $BENCHMARK 10 ""   $BOGOMIPS
benchmark $BENCHMARK 10 "-t" $BOGOMIPS
benchmark $BENCHMARK 11 ""   $BOGOMIPS
benchmark $BENCHMARK 11 "-t" $BOGOMIPS
benchmark $BENCHMARK 12 ""   $BOGOMIPS
benchmark $BENCHMARK 12 "-t" $BOGOMIPS

rm time.out

echo $FREQ > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq

cd $PWD
