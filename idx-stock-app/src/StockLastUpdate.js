import React, { useState, useEffect } from 'react';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Box from '@mui/material/Box';
import { DataGrid } from '@mui/x-data-grid';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import RemoveIcon from '@mui/icons-material/Remove';
import Typography from '@mui/material/Typography';
import Avatar from '@mui/material/Avatar';
import Link from "@mui/material/Link";

function App() {
    const [data, setData] = useState([]);
    const [keyword, setKeyword] = useState('');

    // GET with fetch API
    useEffect(() => {

        const fetchData = async () => {
            const response = await fetch('http://localhost:9000/allStock');
            const result = await response.json();
            setData(result);
        };
        // fetchData();

        const interval = setInterval(() => {
            fetchData();
        }, 1000); //Interval every x ms

        return () => {
            clearInterval(interval);
        };

    }, []);

    // const findHistoricalTicker = async (keyword) => {
    //     var ticker = keyword.toUpperCase();
    //     console.log("search ticker: " + ticker)
    //     const fetchData = async () => {
    //         const response = await fetch(
    //             'http://localhost:9090/ticker/' + ticker
    //         );
    //         const data = await response.json();
    //         console.log(data);
    //         setData(data);
    //     };
    //     fetchData();
    // };

    // const handleSubmit = (e) => {
    //     e.preventDefault();
    //     findHistoricalTicker(keyword);
    // };

    const columns = [
        { field: 'id', headerName: 'ID', width: 150 },
        {
            field: 'logo',
            headerName: 'Logo',
            width: 100,
            editable: true,
            renderCell: (params) =>
                <Link href={params.row.logo}>
                    <Avatar alt={params.row.name} variant="rounded" src={params.row.logo} />
                </Link>
        },
        {
            field: 'ticker',
            headerName: 'Ticker',
            width: 100,
            editable: true,
        },
        {
            field: 'date',
            headerName: 'Date',
            width: 100,
            editable: true,
        },
        {
            field: 'volume',
            headerName: 'Volume',
            type: 'number',
            width: 120,
            editable: true,
        },
        {
            field: 'open',
            headerName: 'Open',
            type: 'number',
            width: 100,
            editable: true,
        },
        {
            field: 'high',
            headerName: 'High',
            type: 'number',
            width: 100,
            editable: true,
        },
        {
            field: 'low',
            headerName: 'Low',
            type: 'number',
            width: 100,
            editable: true,
        },
        {
            field: 'close',
            headerName: 'Close',
            type: 'number',
            width: 100,
            editable: true,
        },
        {
            field: "status",
            headerName: "Status",
            sortable: false,
            headerAlign: 'center',
            align: 'center',
            width: 200,
            disableClickEventBubbling: true,
            renderCell: (params) => {
                return (
                    <div
                        className="d-flex justify-content-between align-items-center"
                        style={{ cursor: "pointer" }}
                    >
                        <MatUpDown id={params.row.id} index={params.row} />
                    </div>
                );
            }
        }
    ];

    const MatUpDown = ({ index }) => {
        if (index.change > 0) {
            return (
                <Box sx={{
                    display: 'flex',
                    color: 'success.main'
                }}>
                    <ArrowUpwardIcon sx={{ m: 0.5 }} />
                    <Typography sx={{ m: 0.5 }} variant="button" display="block" gutterBottom>
                        {index.changeval}
                    </Typography>
                    <Typography sx={{ m: 0.5 }} variant="button" display="block" gutterBottom>
                        {index.changepercent}
                    </Typography>

                </Box>
            );
        } else if (index.change < 0) {
            return (
                <Box sx={{
                    display: 'flex',
                    color: 'error.main'
                }}>
                    <ArrowDownwardIcon sx={{ m: 0.5 }} />
                    <Typography sx={{ m: 0.5 }} variant="button" display="block" gutterBottom>
                        {index.changeval}
                    </Typography>
                    <Typography sx={{ m: 0.5 }} variant="button" display="block" gutterBottom>
                        {index.changepercent}
                    </Typography>
                </Box>
            );
        } else {
            return (
                <Box sx={{
                    display: 'flex',
                }}>
                    <RemoveIcon sx={{ m: 0.5 }} />
                    <Typography sx={{ m: 0.5 }} variant="button" display="block" gutterBottom>
                        {index.changeval}
                    </Typography>
                    <Typography sx={{ m: 0.5 }} variant="button" display="block" gutterBottom>
                        {index.changepercent}
                    </Typography>
                </Box>
            );
        }
    };

    const handleRowClick: GridEventListener<'rowClick'> = (params) => {
        // window.location.href = 'https://google.com';
    };

    return (
        <Box sx={{ flexGrow: 1 }}>
            {/* <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'flex-start',
                    p: 1,
                    m: 1,
                    bgcolor: 'background.paper',
                    borderRadius: 1,
                }}>
                <form onSubmit={handleSubmit}>
                    <Box component="span" sx={{ p: 0 }}>
                        <TextField size="small" id="outlined-basic" label="Ticker Name" variant="outlined" type="text" className="form-control" id="" value={keyword}
                            onChange={(e) => setKeyword(e.target.value)}
                        />
                    </Box>
                    <Box component="span" sx={{ p: 1 }}>
                        <Button size="medium" variant="contained" type="submit">Search History Ticker</Button>
                    </Box>
                </form>
            </Box> */}
            <Box sx={{ height: 480, width: '100%' }}>
                <DataGrid
                    onRowClick={handleRowClick}
                    rows={data}
                    columns={columns}
                    initialState={{
                        columns: {
                            columnVisibilityModel: {
                                id: false,
                            }
                        },
                        pagination: {
                            paginationModel: {
                                pageSize: 7,
                            },
                        },
                    }}
                    pageSizeOptions={[5]}
                    checkboxSelection
                    disableRowSelectionOnClick
                />
            </Box>
        </Box >
    );
};

export default App;
