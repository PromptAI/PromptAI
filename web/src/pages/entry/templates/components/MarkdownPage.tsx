import React, {useState} from 'react';
import Markdown from 'react-markdown';
import ReactPaginate from "react-paginate";

function MarkdownPage({ markdown}) {
    const pages = markdown.split('---');

    const [currentPage, setCurrentPage] = useState(0);
    const itemsPerPage = 1;

    const handlePageClick = (data) => {
        setCurrentPage(data.selected);
    };

    const offset = currentPage * itemsPerPage;
    const currentItems = pages.slice(offset, offset + itemsPerPage);

    return (
        <div className="">
            {currentItems.map((item, index) => (
                <div key={index} className={"overflow-hidden"} style={{height: '590px', width: '830px'}}>
                    {/*<ReactMarkdown>{item}</ReactMarkdown>*/}
                    <Markdown className="prose prose-xl max-w-none pr-2 "  >{item}</Markdown>
                </div>

            ))}
            <ReactPaginate
                previousLabel={'← Previous'}
                nextLabel={'Next →'}
                breakLabel={''}
                breakClassName={''}
                pageCount={Math.ceil(pages.length / itemsPerPage)}
                marginPagesDisplayed={0}
                pageRangeDisplayed={0}
                onPageChange={handlePageClick}
                containerClassName={'flex justify-between mt-4'}
                // previousLinkClassName={'px-3 arco-btn-primary text-whit py-1 border border-gray-300 rounded hover:bg-gray-200'}
                // nextLinkClassName={'px-3 py-1 arco-btn-primary border border-gray-300 rounded hover:bg-gray-200'}
                previousLinkClassName={'px-3 py-1   rounded hover:bg-gray-200'}
                nextLinkClassName={'px-3 py-1  rounded hover:bg-gray-200'}
                activeClassName={'text-blue'}
                activeLinkClassName={'text-blue'}
                disabledClassName={'text-gray-400 '}
            />
        </div>
    );
}

export default MarkdownPage;