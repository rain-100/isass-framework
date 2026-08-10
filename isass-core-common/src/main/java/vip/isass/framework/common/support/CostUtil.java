// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * 耗时计算工具
 *
 * @author Rain
 */
public class CostUtil {

    public static void compute(int timeOfEachGroup, int group, Predicate<Boolean> predicate) {
        compute(timeOfEachGroup, group, predicate, TimeUnit.MILLISECONDS);
    }

    /**
     * 每组执行n次计算逻辑，一共执行m组。总计执行 n * m 次计算逻辑
     * 每组的执行结果，去掉最大值和最小值后取平均数
     * 得到每个组的平均数后，再去掉最大值和最小值后取平均数，得出计算逻辑的耗时
     *
     * @param timeOfEachGroup time of each group
     * @param group           how many group
     * @param predicate       predicate
     * @param timeUnit        time unit
     */
    public static void compute(int timeOfEachGroup, int group, Predicate<Boolean> predicate, TimeUnit timeUnit) {
        if (timeOfEachGroup < 3) {
            throw new IllegalArgumentException("n 必须大于等于3");
        }
        if (group < 3) {
            throw new IllegalArgumentException("m 必须大于等于3");
        }
        switch (timeUnit) {
            case MILLISECONDS:
            case NANOSECONDS:
                break;
            default:
                throw new UnsupportedOperationException("暂不支持的timeUnit：" + timeUnit);
        }

        System.out.println("耗时单位：" + timeUnit);
        // 执行 m 组的结果
        long[][] mGroupRecords = new long[group][timeOfEachGroup];

        for (int i = 0; i < group; i++) {
            mGroupRecords[i] = computeGroup(timeOfEachGroup, predicate, timeUnit);
        }

        removeMaxAndMin(mGroupRecords);

        long[] avgs = avg(mGroupRecords);

        System.out.println("每组的平均值：");
        printGroup(0, avgs);

        // 去掉最大值
        GetMaxAndMin getMaxAndMin = new GetMaxAndMin(avgs).invoke();
        long min = getMaxAndMin.getMin();
        long max = getMaxAndMin.getMax();
        long[] groupRecords = getMaxAndMin.getGroupRecords();
        killMaxAndMin(min, max, groupRecords);
        System.out.println("去掉最大值每组的平均值：");
        printGroup(group, groupRecords);

        System.out.print("总平均值：");
        long totalCost = 0;
        for (long avg : avgs) {
            totalCost += avg;
        }
        System.out.println(totalCost / (avgs.length - 2));
    }

    /**
     * 计算每组的平均值
     *
     * @param mGroupRecords m group records
     * @return avg cost
     */
    private static long[] avg(long[][] mGroupRecords) {
        long[] avgs = new long[mGroupRecords.length];

        for (int m = 0; m < mGroupRecords.length; m++) {
            long[] groupRecords = mGroupRecords[m];
            long total = 0;
            for (long groupRecord : groupRecords) {
                total += groupRecord;
            }
            avgs[m] = total / (groupRecords.length - 2);
        }
        return avgs;
    }

    private static void removeMaxAndMin(long[][] mGroupRecords) {

        for (int m = 0; m < mGroupRecords.length; m++) {
            // 当前组的最大值最小值
            GetMaxAndMin getMaxAndMin = new GetMaxAndMin(mGroupRecords[m]).invoke();
            long min = getMaxAndMin.getMin();
            long max = getMaxAndMin.getMax();
            long[] groupRecords = getMaxAndMin.getGroupRecords();
            printGroup(m, groupRecords);

            killMaxAndMin(min, max, groupRecords);

            printGroup(m, groupRecords);
        }
    }

    private static void printGroup(int m, long[] groupRecords) {
        System.out.print("第" + m + "组结果：[ ");
        for (long groupRecord : groupRecords) {
            System.out.print(groupRecord + ", ");
        }
        System.out.println(" ]");
    }

    /**
     * 去掉一组中的最大值和最小值
     *
     * @param min          min
     * @param max          max
     * @param groupRecords group records
     */
    private static void killMaxAndMin(long min, long max, long[] groupRecords) {
        // 去掉最大值最小值
        for (int j = 0; j < groupRecords.length; j++) {
            if (min != -1) {
                if (groupRecords[j] == min) {
                    groupRecords[j] = 0;
                    min = -1;
                }
            }

            if (max != -1) {
                if (groupRecords[j] == max) {
                    groupRecords[j] = 0;
                    max = -1;
                }
            }
        }
    }

    /**
     * 计算一组的耗时
     */
    private static long[] computeGroup(int n, Predicate<Boolean> predicate, TimeUnit timeUnit) {
        long[] records = new long[n];
        long start;
        long cost;
        for (int i = 0; i < n; i++) {
            switch (timeUnit) {
                case MILLISECONDS:
                    start = System.currentTimeMillis();
                    break;
                case NANOSECONDS:
                    start = System.nanoTime();
                    break;
                default:
                    start = 0L;
            }
            predicate.test(Boolean.TRUE);
            switch (timeUnit) {
                case MILLISECONDS:
                    cost = System.currentTimeMillis() - start;
                    break;
                case NANOSECONDS:
                    cost = System.nanoTime() - start;
                    break;
                default:
                    cost = 0L;
            }

            records[i] = cost;
        }
        return records;
    }

    private static class GetMaxAndMin {
        private long[] mGroupRecord;
        private long min;
        private long max;
        private long[] groupRecords;

        public GetMaxAndMin(long... mGroupRecord) {
            this.mGroupRecord = mGroupRecord;
        }

        public long getMin() {
            return min;
        }

        public long getMax() {
            return max;
        }

        public long[] getGroupRecords() {
            return groupRecords;
        }

        public GetMaxAndMin invoke() {
            groupRecords = mGroupRecord;
            min = groupRecords[0];
            max = groupRecords[0];
            for (long groupRecord : groupRecords) {
                if (groupRecord > max) {
                    max = groupRecord;
                }

                if (groupRecord < min) {
                    min = groupRecord;
                }
            }
            return this;
        }
    }

    }
