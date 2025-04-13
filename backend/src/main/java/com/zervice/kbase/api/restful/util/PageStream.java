package com.zervice.kbase.api.restful.util;

import com.zervice.kbase.api.restful.pojo.PageRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class PageStream {


    // helper map to cache
    private static ConcurrentHashMap<ImmutablePair<Class, String>, Comparator> comparatorCache = new ConcurrentHashMap<>();

    /**
     * 当需要一个特殊的comparator时使用.
     * 注意这个comparator会在clz的fieldName的值上使用
     * @param clz
     * @param fieldName
     * @param c  comparator
     */
    public static void registerSpecialComparator(Class clz, String fieldName, Comparator c) {
        String getMethod = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        try {
            Method m = clz.getDeclaredMethod(getMethod);
            m.setAccessible(true);
            comparatorCache.put(ImmutablePair.of(clz, fieldName), (o1, o2) -> {
                try {
                    return c.compare(m.invoke(o1), m.invoke(o2));
                }
                catch (Exception e) {
                    LOG.warn("Fail to call method {} for class {} when sorting", m, clz, e);
                    return 0;
                }
            });
        }
        catch (Exception e) {
            throw new IllegalArgumentException(String.format("The method %s not exists for class %s", fieldName, clz),
                    e);
        }
    }

    /**
     * get the list of result according to the page request
     * the size, sort in page request is processed
     *
     * @param inClass     the object class in the input stream
     * @param pageRequest page request
     * @param input       input stream
     * @param <IN>
     * @return
     */
    public static <IN> List<IN> of(Class<IN> inClass, PageRequest pageRequest, Stream<IN> input) {
        if (pageRequest.hasSort()) {
            PageRequest.SortParam sortParam = pageRequest.getSortParam();
            Comparator c = comparatorCache.computeIfAbsent(ImmutablePair.of(inClass, sortParam.getSortBy()), pair -> {
                String sortByField = sortParam.getSortBy();
                String sortByMethodName = "get" + sortByField.substring(0, 1).toUpperCase() + sortByField.substring(1);
                Method m;
                try {
                    m = inClass.getDeclaredMethod(sortByMethodName);
                    m.setAccessible(true);
                }
                catch (Exception e) {
                    throw new IllegalArgumentException(String.format("The method %s not exists for class %s", sortByMethodName, inClass),
                            e);
                }
                return (o1, o2) -> {
                    try {
                        return ((Comparable) m.invoke(o1)).compareTo(m.invoke(o2));
                    }
                    catch (Exception e) {
                    }
                    return 0;
                };
            });

            input = input.sorted(sortParam.isAsc() ? c : c.reversed());
        }
        if (pageRequest.isGetAll()) {
            return input.collect(Collectors.toList());
        }
        else {
            return input
                    .skip(pageRequest.getSize() * pageRequest.getPage())
                    .limit(pageRequest.getSize())
                    .collect(Collectors.toList());
        }
    }
//
//    private static RestKb testKb(long i, long time) {
//        RestKb kb = new RestKb();
//        kb.setId(i);
//        kb.setQuery("q" + i);
//        kb.setCreateTime(time);
//        return kb;
//    }
//
//    public static void main(String[] args) {
//        PageRequest r = new PageRequest(0, -1, "createTime,asc");
//        List<RestKb> kbs = Arrays.asList(
//                testKb(1L, 123),
//                testKb(3L, 110),
//                testKb(2L, 135));
//
//        List<RestKb> h = PageStream.of2(RestKb.class, r, kbs.stream());
//        h.stream().forEach(k -> System.out.println(k.getId() + " " + k.getCreateTime()));
//
//    }


//    public static <T> List<T> of(PageRequest pageRequest, Stream<T> input) {
//        if (pageRequest.isGetAll()) {
//            return input.collect(Collectors.toList());
//        }
//        else {
//            return input
//                    .skip(pageRequest.getSize() * pageRequest.getPage())
//                    .limit(pageRequest.getSize())
//                    .collect(Collectors.toList());
//        }
//    }

}
