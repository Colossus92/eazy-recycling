import { PaginationButton } from './PaginationButton';
import CaretDoubleRight from '@/assets/icons/CaretDoubleRight.svg?react';
import CaretLeft from '@/assets/icons/CaretLeft.svg?react';
import CaretRight from '@/assets/icons/CaretRight.svg?react';
import CaretDoubleLeft from '@/assets/icons/CaretDoubleLeft.svg?react';

interface PaginationRowProps {
  page: number;
  setPage: (page: number) => void;
  rowsPerPage: number;
  setRowsPerPage: (rowsPerPage: number) => void;
  numberOfResults: number;
  /**
   * Total number of pages (for server-side pagination).
   * If not provided, calculated from numberOfResults / rowsPerPage.
   */
  totalPages?: number;
}

export const PaginationRow = ({
  page,
  setPage,
  rowsPerPage,
  setRowsPerPage,
  numberOfResults,
  totalPages: totalPagesProp,
}: PaginationRowProps) => {
  // Use provided totalPages for server-side pagination, otherwise calculate
  const numberOfPages = totalPagesProp ?? Math.ceil(numberOfResults / rowsPerPage);
  const getPageNumbers = () => {
    const maxVisiblePages = 5;
    const pageNumbers = [];

    if (numberOfPages <= maxVisiblePages) {
      // If we have 5 or fewer pages, show all of them
      for (let i = 1; i <= numberOfPages; i++) {
        pageNumbers.push(i);
      }
    } else {
      // Always include page 1
      pageNumbers.push(1);

      // We want to show 3 buttons in the middle range
      let leftBound, rightBound;

      if (page <= 3) {
        // Near the start, show pages 2, 3, 4
        leftBound = 2;
        rightBound = 4;
      } else if (page >= numberOfPages - 2) {
        // Near the end, show the three pages before the last page
        leftBound = numberOfPages - 3;
        rightBound = numberOfPages - 1;
      } else {
        // In the middle, center around the current page
        leftBound = page - 2;
        rightBound = page + 2;
      }

      // Add ellipsis if there's a gap between 1 and the leftBound
      if (leftBound > 2) {
        pageNumbers.push('ellipsis-left');
      }

      // Add the pages in the middle range
      for (let i = leftBound; i <= rightBound; i++) {
        pageNumbers.push(i);
      }

      // Add ellipsis if there's a gap between rightBound and the last page
      if (rightBound < numberOfPages - 1) {
        pageNumbers.push('ellipsis-right');
      }

      // Always include the last page
      pageNumbers.push(numberOfPages);
    }

    return pageNumbers;
  };

  return (
    <div className="flex flex-1 items-start gap-2">
      <div className="flex flex-1 items-center gap-2">
        <select
          value={rowsPerPage}
          onChange={(e) => {
            setPage(1);
            setRowsPerPage(Number(e.target.value));
          }}
        >
          <option value="10">10</option>
          <option value="25">25</option>
          <option value="50">50</option>
        </select>
        <span className="text-body-2">
          {numberOfPages > 0
            ? `${(page - 1) * rowsPerPage + 1}-${Math.min(page * rowsPerPage, numberOfResults)} van ${numberOfResults} resultaten`
            : '0 resultaten'}
        </span>
      </div>
      <div className="flex items-start gap-2">
        <PaginationButton
          value={<CaretDoubleLeft />}
          isDisabled={page === 1}
          onClick={() => setPage(1)}
        />
        <PaginationButton
          value={<CaretLeft />}
          isDisabled={page === 1}
          onClick={() => setPage(page - 1)}
        />

        {getPageNumbers().map((pageNumber, index) => {
          if (
            pageNumber === 'ellipsis-left' ||
            pageNumber === 'ellipsis-right'
          ) {
            return (
              <PaginationButton
                key={`${pageNumber}-${index}`}
                value={'…'}
                isDisabled={true}
                onClick={() => {}}
              />
            );
          }

          return (
            <PaginationButton
              key={`page-${pageNumber}`}
              value={pageNumber}
              isActive={pageNumber === page}
              onClick={() => setPage(Number(pageNumber))}
            />
          );
        })}

        <PaginationButton
          value={<CaretRight />}
          isDisabled={page === numberOfPages || numberOfPages === 0}
          onClick={() => setPage(page + 1)}
        />
        <PaginationButton
          value={<CaretDoubleRight />}
          isDisabled={page === numberOfPages || numberOfPages === 0}
          onClick={() => setPage(numberOfPages)}
        />
      </div>
    </div>
  );
};
