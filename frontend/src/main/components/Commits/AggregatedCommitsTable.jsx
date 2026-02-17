import React, { useMemo, useState } from "react";
import {
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table";
import { Form } from "react-bootstrap";
import SortCaret from "main/components/Common/SortCaret";

const columns = [
  {
    header: "SHA",
    accessorKey: "sha",
    id: "sha",
    cell: ({ cell }) => cell.getValue()?.substring(0, 7),
  },
  {
    header: "Message",
    accessorKey: "message",
    id: "message",
  },
  {
    header: "Commit Time",
    accessorKey: "commitTime",
    id: "commitTime",
    cell: ({ cell }) => {
      const value = cell.getValue();
      return value ? new Date(value).toLocaleString() : "";
    },
  },
  {
    header: "Committer Name",
    accessorKey: "committerName",
    id: "committerName",
  },
  {
    header: "Committer Email",
    accessorKey: "committerEmail",
    id: "committerEmail",
  },
  {
    header: "Committer Login",
    accessorKey: "committerLogin",
    id: "committerLogin",
  },
  {
    header: "Author Name",
    accessorKey: "authorName",
    id: "authorName",
  },
  {
    header: "Author Email",
    accessorKey: "authorEmail",
    id: "authorEmail",
  },
  {
    header: "Author Login",
    accessorKey: "authorLogin",
    id: "authorLogin",
  },
  {
    header: "URL",
    accessorKey: "url",
    id: "url",
  },
];

function ColumnFilter({ column, testid }) {
  const columnFilterValue = column.getFilterValue() ?? "";
  return (
    <Form.Control
      type="text"
      size="sm"
      value={columnFilterValue}
      onChange={(e) => column.setFilterValue(e.target.value)}
      placeholder={`Filter...`}
      data-testid={`${testid}-filter-${column.id}`}
    />
  );
}

export default function AggregatedCommitsTable({ commits }) {
  const testid = "AggregatedCommitsTable";
  const [columnFilters, setColumnFilters] = useState([]);
  const data = useMemo(() => commits, [commits]);

  const table = useReactTable({
    data,
    columns,
    state: { columnFilters },
    onColumnFiltersChange: setColumnFilters,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  return (
    <table className="table table-striped table-bordered" data-testid={testid}>
      <thead>
        {table.getHeaderGroups().map((headerGroup, i) => (
          <tr
            data-testid={`${testid}-header-group-${i}`}
            key={`${testid}-header-group-${i}`}
          >
            {headerGroup.headers.map((header) => (
              <th
                data-testid={`${testid}-header-${header.column.id}`}
                key={`${testid}-header-${header.column.id}`}
                colSpan={header.colSpan}
              >
                {header.isPlaceholder ? null : (
                  <>
                    <div
                      {...(header.column.getCanSort() && {
                        onClick: header.column.getToggleSortingHandler(),
                        style: { cursor: "pointer" },
                      })}
                      data-testid={`${testid}-header-${header.column.id}-sort-header`}
                    >
                      {flexRender(
                        header.column.columnDef.header,
                        header.getContext(),
                      )}
                      <SortCaret header={header} testId={testid} />
                    </div>
                    {header.column.getCanFilter() && (
                      <ColumnFilter column={header.column} testid={testid} />
                    )}
                  </>
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
            <tr data-testid={rowTestId} key={rowTestId}>
              {row.getVisibleCells().map((cell) => {
                const cellTestId = `${testid}-cell-row-${cell.row.index}-col-${cell.column.id}`;
                return (
                  <td data-testid={cellTestId} key={cellTestId}>
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
