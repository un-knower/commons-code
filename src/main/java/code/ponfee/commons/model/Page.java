package code.ponfee.commons.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;

/**
 * 参考guthub开源的mybatis分页工具
 *   项目地址：http://git.oschina.net/free/Mybatis_PageHelper
 *   属性配置：http://bbs.csdn.net/topics/360010907
 * 
 * @author fupf
 */
public class Page<T> implements java.io.Serializable {
    private static final long serialVersionUID = 1313118491812094979L;

    private int pageNum; // 当前页
    private int pageSize; // 每页的数量
    private int size; // 当前页的数量
    private int startRow; // 当前页面第一个元素在数据库中的行号
    private int endRow; // 当前页面最后一个元素在数据库中的行号
    private long total; // 总记录数
    private int pages; // 总页数
    private List<T> rows; // 结果集
    private int prePage; // 前一页
    private int nextPage; // 下一页
    private boolean isFirstPage = false; // 是否为第一页
    private boolean isLastPage = false; // 是否为最后一页
    private boolean hasPreviousPage = false; // 是否有前一页
    private boolean hasNextPage = false; // 是否有下一页
    private int navigatePages; // 导航页码数
    private int[] navigatePageNums; // 所有导航页号
    private int navigateFirstPage; // 导航条上的第一页
    private int navigateLastPage; // 导航条上的最后一页

    public Page() {
        this(new ArrayList<>());
    }

    /**
     * 包装Page对象
     * @param list
     */
    public Page(List<T> list) {
        this(list, 8);
    }

    /**
     * 包装Page对象
     * @param list          page结果
     * @param navigatePages 页码数量
     */
    public Page(List<T> list, int navigatePages) {
        if (list instanceof com.github.pagehelper.Page) {
            com.github.pagehelper.Page<T> page = (com.github.pagehelper.Page<T>) list;
            this.pageNum = page.getPageNum();
            this.pageSize = page.getPageSize();

            this.pages = page.getPages();
            this.rows = copy(page);
            this.size = page.size();
            this.total = page.getTotal();
            if (this.size == 0) {
                this.startRow = 0;
                this.endRow = 0;
            } else {
                this.startRow = page.getStartRow() + 1; // 由于结果是>startRow的，所以实际的需要+1
                this.endRow = this.startRow - 1 + this.size; // 计算实际的endRow（最后一页的时候特殊）
            }
        } else {
            if (list == null) {
                list = new ArrayList<>();
            }
            this.pageNum = 1;
            this.pageSize = list.size();

            this.pages = this.pageSize > 0 ? 1 : 0;
            this.rows = list;
            this.size = list.size();
            this.total = list.size();
            this.startRow = 0;
            this.endRow = list.size() > 0 ? list.size() - 1 : 0;
        }

        this.navigatePages = navigatePages;
        calcNavigatePageNums(); // 计算导航页
        calcPage(); // 计算前后页，第一页，最后一页
        judgePageBoudary(); // 判断页面边界
    }

    private List<T> copy(com.github.pagehelper.Page<T> page) {
        return Lists.newArrayList(page);
    }

    /**
     * 计算导航页
     */
    private void calcNavigatePageNums() {
        if (pages <= navigatePages) { // 当总页数小于或等于导航页码数时
            navigatePageNums = new int[pages];
            for (int i = 0; i < pages; i++) {
                navigatePageNums[i] = i + 1;
            }
        } else { // 当总页数大于导航页码数时
            navigatePageNums = new int[navigatePages];
            int startNum = pageNum - navigatePages / 2;
            int endNum = pageNum + navigatePages / 2;

            if (startNum < 1) {
                startNum = 1;
                // (最前navigatePages页
                for (int i = 0; i < navigatePages; i++) {
                    navigatePageNums[i] = startNum++;
                }
            } else if (endNum > pages) {
                endNum = pages;
                // 最后navigatePages页
                for (int i = navigatePages - 1; i >= 0; i--) {
                    navigatePageNums[i] = endNum--;
                }
            } else {
                // 所有中间页
                for (int i = 0; i < navigatePages; i++) {
                    navigatePageNums[i] = startNum++;
                }
            }
        }
    }

    /**
     * 计算前后页，第一页，最后一页
     */
    private void calcPage() {
        if (navigatePageNums != null && navigatePageNums.length > 0) {
            navigateFirstPage = navigatePageNums[0];
            navigateLastPage = navigatePageNums[navigatePageNums.length - 1];
            if (pageNum > 1) {
                prePage = pageNum - 1;
            }
            if (pageNum < pages) {
                nextPage = pageNum + 1;
            }
        }
    }

    /**
     * 判定页面边界
     */
    private void judgePageBoudary() {
        isFirstPage = pageNum == 1;
        isLastPage = pageNum == pages;
        hasPreviousPage = pageNum > 1;
        hasNextPage = pageNum < pages;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getStartRow() {
        return startRow;
    }

    public boolean isFirstPage() {
        return isFirstPage;
    }

    public void setFirstPage(boolean isFirstPage) {
        this.isFirstPage = isFirstPage;
    }

    public boolean isLastPage() {
        return isLastPage;
    }

    public void setLastPage(boolean isLastPage) {
        this.isLastPage = isLastPage;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
    }

    public int getEndRow() {
        return endRow;
    }

    public void setEndRow(int endRow) {
        this.endRow = endRow;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public List<T> getRows() {
        return rows;
    }

    public void setRows(List<T> rows) {
        this.rows = rows;
    }

    public int getPrePage() {
        return prePage;
    }

    public void setPrePage(int prePage) {
        this.prePage = prePage;
    }

    public int getNextPage() {
        return nextPage;
    }

    public void setNextPage(int nextPage) {
        this.nextPage = nextPage;
    }

    public boolean isHasPreviousPage() {
        return hasPreviousPage;
    }

    public void setHasPreviousPage(boolean hasPreviousPage) {
        this.hasPreviousPage = hasPreviousPage;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setHasNextPage(boolean hasNextPage) {
        this.hasNextPage = hasNextPage;
    }

    public int getNavigatePages() {
        return navigatePages;
    }

    public void setNavigatePages(int navigatePages) {
        this.navigatePages = navigatePages;
    }

    public int[] getNavigatePageNums() {
        return navigatePageNums;
    }

    public void setNavigatePageNums(int[] navigatePageNums) {
        this.navigatePageNums = navigatePageNums;
    }

    public int getNavigateFirstPage() {
        return navigateFirstPage;
    }

    public int getNavigateLastPage() {
        return navigateLastPage;
    }

    public void setNavigateFirstPage(int navigateFirstPage) {
        this.navigateFirstPage = navigateFirstPage;
    }

    public void setNavigateLastPage(int navigateLastPage) {
        this.navigateLastPage = navigateLastPage;
    }

    /**
     * 处理
     * @param action
     */
    public void process(Consumer<T> action) {
        if (rows == null || rows.isEmpty() || action == null) {
            return;
        }
        rows.forEach(action);
    }

    /**
     * 转换
     * @param mapper
     * @return
     */
    public <E> Page<E> transform(Function<T, E> mapper) {
        Page<E> page = new Page<>();
        BeanUtils.copyProperties(this, page);
        if (rows == null || rows.isEmpty() || mapper == null) {
            return page;
        }
        page.setRows(rows.stream().map(mapper).collect(Collectors.toList()));
        return page;
    }

    @Override
    public String toString() {
        return new StringBuilder(280)
            .append(getClass().getCanonicalName()).append("@")
            .append(Integer.toHexString(hashCode())).append("{")
            .append("pageNum=").append(pageNum)
            .append(",pageSize=").append(pageSize)
            .append(",size=").append(size)
            .append(",startRow=").append(startRow)
            .append(",endRow=").append(endRow)
            .append(",total=").append(total)
            .append(",pages=").append(pages)
            .append(",rows=").append(rowsToString())
            .append(",prePage=").append(prePage)
            .append(",nextPage=").append(nextPage)
            .append(",navigatePages=").append(navigatePages)
            .append(",navigatePageNums=").append(Arrays.toString(navigatePageNums))
            .append("}").toString();
    }

    private String rowsToString() {
        // GenericUtils.getFieldGenericType(ClassUtils.getField(Page.class, "rows"))
        if (rows.isEmpty()) {
            return "List<T>(0)";
        }

        T first = rows.get(0);
        if (first == null) {
            return "List<T>(" + rows.size() + ")";
        }

        return "List<" + first.getClass().getCanonicalName() + ">(" + rows.size() + ")";
    }

    public static void main(String[] args) {
        List<String> list = Arrays.asList("1","2","3","4","5","6","7","8","9","0");
        System.out.println(new Page<>(list).toString());
        System.out.println(new Page<>().toString());
        System.out.println(new Page<>(Arrays.asList(null,1)).toString());
    }
}
