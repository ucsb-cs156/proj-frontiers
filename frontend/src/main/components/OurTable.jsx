import React, { useMemo, useState } from "react";
import {
  createColumnHelper,
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table";
import { Button } from "react-bootstrap";
import SortCaret from "main/components/Common/SortCaret";
import { convertOldStyleColumnsToNewStyle } from "main/components/OurTableUtils";

function OurTable({ data, columns, testid = "testid" }) {
  const [columnFilters, setColumnFilters] = useState([]);
  const newColumns = convertOldStyleColumnsToNewStyle(columns);
  const memoizedData = useMemo(() => data, [data]);
  const memoizedColumns = useMemo(() => newColumns, [newColumns]);

  const table = useReactTable({
    data: memoizedData,
    columns: memoizedColumns,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getSortedRowModel: getSortedRowModel(),
    enableColumnFilters: true,
    state: { columnFilters },
    onColumnFiltersChange: setColumnFilters,
  });

  return (
    <table className="table table-striped table-bordered" data-testid={testid}>
      <thead>
        {table.getHeaderGroups().map((headerGroup, i) => (
          <tr
            data-testid={`${testid}-header-group-${i}`}
            // Stryker disable next-line StringLiteral : React key property not exposed in dom
            key={`${testid}-header-group-${i}`}
          >
            {headerGroup.headers.map((header) => (
              <th
                data-testid={`${testid}-header-${header.column.id}`}
                key={`${testid}-header-${header.column.id}`}
                colSpan={header.colSpan}
              >
                {header.isPlaceholder ? null : (
                  <div
                    // Add onClick handler for sorting if the column is sortable
                    {...(header.column.getCanSort() && {
                      onClick: header.column.getToggleSortingHandler(),
                      style: { cursor: "pointer" }, // Add cursor style for visual cue
                    })}
                    data-testid={`${testid}-header-${header.column.id}-sort-header`}
                  >
                    {flexRender(
                      header.column.columnDef.header,
                      header.getContext(),
                    )}
                    <SortCaret header={header} testId={testid} />
                  </div>
                )}
                {header.column.getCanFilter() && (
                  <input
                    type="text"
                    placeholder={`Filter...`}
                    value={header.column.getFilterValue() ?? ""}
                    onChange={(e) =>
                      header.column.setFilterValue(e.target.value)
                    }
                    onClick={(e) => e.stopPropagation()}
                    data-testid={`${testid}-header-${header.column.id}-filter`}
                    className="form-control form-control-sm mt-1"
                  />
                )}
              </th>
            ))}
          </tr>
        ))}
      </thead>
      <tbody>
        {table.getRowModel().rows.map((row) => {
          const rowTestId = `${testid}-row-${row.index}`;
          return (
            <tr
              data-testid={rowTestId}
              // Stryker disable next-line StringLiteral : React key property not exposed in dom
              key={rowTestId}
            >
              {row.getVisibleCells().map((cell) => {
                const testId = `${testid}-cell-row-${cell.row.index}-col-${cell.column.id}`;
                return (
                  <td
                    data-testid={testId}
                    // Stryker disable next-line StringLiteral : React key property not exposed in dom
                    key={testId}
                  >
                    {flexRender(cell.column.columnDef.cell, cell.getContext())}
                  </td>
                );
              })}
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

export default OurTable;

export function ButtonColumn(label, variant, callback, testid) {
  const columnHelper = createColumnHelper();

  const buttonColumn = columnHelper.display({
    id: label, // Unique ID for display columns
    header: label,
    cell: ({ cell }) => (
      <Button
        variant={variant}
        onClick={() => callback(cell)}
        data-testid={`${testid}-cell-row-${cell.row.index}-col-${cell.column.id}-button`}
      >
        {label}
      </Button>
    ),
  });
  return buttonColumn;
}
